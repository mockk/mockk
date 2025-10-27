package io.mockk.core

import io.mockk.core.ValueClassSupport.boxedValue
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinFunction

/**
 * JVM-specific helpers for working with Kotlin value classes in MockK.
 *
 * Responsibilities:
 * - Decide whether to return a value-class instance as-is or its underlying value when crossing the
 *   interception boundary (functions and property getters).
 * - Handle tricky cases where the declared Kotlin return type is a type parameter (generics), which
 *   erases nullability information at runtime.
 * - Avoid invoking value-class property accessors (getters) because those can be mocked/intercepted
 *   and may throw when not stubbed.
 *
 * Key design points:
 * - For generic return types, we resolve Kotlin's synthetic unbox method ("unbox-impl") once per
 *   value class and cache a MethodHandle for it. If the handle returns null for a concrete value,
 *   we propagate null; otherwise we return the instance as-is. All failures to resolve are handled
 *   during cache initialization, so the hot path has no try/catch.
 * - For concrete (non-generic) return types, unbox when the method expects the exact value class
 *   type (with special-cases for primitives, suspend functions, and supertype/interface returns),
 *   otherwise return the instance.
 */
actual object ValueClassSupport {
    private val unboxValueReturnTypes = setOf(Result.success("").javaClass.kotlin)

    /**
     * Decide whether to unbox a value-class result for a specific Java [method] call.
     *
     * Behavior summary:
     * - If `this` is not a value class, return it unchanged.
     * - If the call targets Kotlin's synthetic `*unbox-impl` method, return the underlying value (`boxedValue`).
     * - Determine the declared Kotlin return type (function or property getter):
     *   - If it is a type parameter (i.e., not a concrete [KClass]), treat it as a generic return. In that case,
     *     resolve and use a cached `MethodHandle` to the synthetic `unbox-impl` method of the runtime value-class.
     *     If invoking that handle returns `null`, propagate `null`; otherwise return the instance as-is. This avoids
     *     calling value-class getters while still surfacing logical nulls when `T` is instantiated as `ValueClass?`.
     *   - If it is a concrete type, unbox only when appropriate for the declared return type (with special handling
     *     for primitives, suspend functions, and supertype/interface returns).
     *
     * Generic-handling rationale:
     * - We must not invoke property getters on value classes in this path because those accessors can be mocked.
     *   Touching them would route back through interception and can fail with `MockKException` during
     *   recording/answering if not stubbed.
     * - Using a cached `MethodHandle` moves all reflective failure modes to one-time initialization, keeping this
     *   hot path free of try/catch and reflective exceptions.
     *
     * @param method The Java reflection [Method] being invoked through the proxy.
     * @return Either the original instance or its underlying value, or `null` when the best-effort generic probe
     *         reveals a logical null for `T = ValueClass?`.
     * @see boxedValue
     */
    actual fun <T : Any> T.maybeUnboxValueForMethodReturn(method: Method): Any? {
        val resultType = this::class
        // Don't unbox if not a value class
        if (!resultType.isValue_safe) return this

        // Unbox Kotlin synthetic *unbox-impl methods
        if (method.name.endsWith("unbox-impl")) return this.boxedValue

        val kFunction = method.kotlinFunction
        val kProperty = if (kFunction == null) findMatchingPropertyWithJavaGetter(method) else null

        val expectedReturnType = when {
            kFunction != null -> kFunction.returnType.classifier
            kProperty != null -> kProperty.returnType.classifier
            else -> return this
        }

        // Handle generic type parameters
        if (expectedReturnType !is KClass<*>) {
            val handle = resultType.resolveUnboxHandleOrNull()
            if (handle != null) {
                handle.invoke(this) ?: return null
            }
            return this
        }

        val isReturnNullable = when {
            kFunction != null -> kFunction.returnType.isMarkedNullable
            kProperty != null -> kProperty.returnType.isMarkedNullable
            else -> false
        }

        val isPrimitive = resultType.innermostBoxedClass().java.isPrimitive
        val isExpectedTypeValueClass = expectedReturnType == resultType
        val isExpectedTypeSupertype = expectedReturnType != resultType &&
                expectedReturnType.isSuperclassOf(resultType)

        return when {
            // Don't unbox when returning via supertype/interface
            isExpectedTypeSupertype -> this
            // Don't unbox if nullable primitive or suspend fun with primitive
            isExpectedTypeValueClass && (isReturnNullable && isPrimitive) -> this
            isExpectedTypeValueClass && (kFunction?.isSuspend == true && isPrimitive) -> this
            // Unbox for value class return type
            isExpectedTypeValueClass -> this.boxedValue
            // For property: unbox if not nullable primitive
            kProperty != null && !(isReturnNullable && isPrimitive) -> this.boxedValue
            // Default: don't unbox
            else -> this
        }
    }

    private fun findMatchingPropertyWithJavaGetter(method: Method): KProperty<*>? =
        method.declaringClass.kotlin.declaredMemberProperties.find {
            it.javaGetter == method
        }

    /**
     * Underlying property value of a **`value class`** or self.
     * Includes workaround for [Result] class
     *
     * The type of the return might also be a `value class`!
     */
    actual val <T : Any> T.boxedValue: Any?
        @Suppress("UNCHECKED_CAST")
        get() =
            if (!this::class.isValue_safe) {
                this
            } else {
                val klass = (this::class as KClass<T>)
                val boxedProperty = klass.boxedProperty.get(this)
                if (klass == Result::class) {
                    boxedProperty
                } else {
                    boxedProperty?.boxedValue
                }
            }

    /**
     * Underlying property class of a **`value class`** or self.
     *
     * The returned class might also be a `value class`!
     */
    actual val KClass<*>.boxedClass: KClass<*>
        get() =
            if (!this.isValue_safe) {
                this
            } else {
                this.boxedProperty.returnType.classifier as KClass<*>
            }

    /**
     * Underlying property class of a **`value class`** or self.
     * When the value class has one or more nested value classes,
     * the innermost boxed class is returned (up to a maximum depth to prevent infinite recursion).
     *
     * @param maxDepth Maximum recursion depth (default 10)
     */
    fun KClass<*>.innermostBoxedClass(maxDepth: Int = 10): KClass<*> {
        if (maxDepth <= 0 || !this.isValue_safe) {
            return this
        }
        val boxed = this.boxedClass
        return if (boxed == this) {
            this
        } else {
            boxed.innermostBoxedClass(maxDepth - 1)
        }
    }

    private val valueClassFieldCache = mutableMapOf<KClass<out Any>, KProperty1<out Any, *>>()

    /**
     * Cached MethodHandle to the synthetic Kotlin unbox method ("unbox-impl") per value class.
     *
     * Why MethodHandle?
     * - We want to avoid `Method.invoke` on the hot path because it can wrap/propagate reflective
     *   exceptions. By resolving once and storing a handle, the hot path is exception-free.
     *
     * Failure policy:
     * - If resolution fails for any reason (no such method, access restrictions, unusual runtime),
     *   we cache `null` and simply skip probing for that class thereafter (return instance as-is for
     *   generics).
     */
    private val valueClassUnboxHandleCache = mutableMapOf<KClass<out Any>, MethodHandle?>()

    /**
     * Lookup used to unreflect the discovered method into a MethodHandle (lazily created).
     */
    private val methodHandleLookup by lazy { MethodHandles.lookup() }

    /**
     * Resolve and cache a `MethodHandle` for this value class's zero-arg synthetic `unbox-impl` method.
     * Returns `null` if the class is not a value class or if resolution fails.
     *
     * All reflective errors are handled during initialization; the hot path never throws.
     */
    private fun <T : Any> KClass<T>.resolveUnboxHandleOrNull(): MethodHandle? {
        // There's no such method for non-value classes
        if (!this.isValue_safe) return null

        return valueClassUnboxHandleCache.getOrPut(this) {
            try {
                // Find zero-arg Kotlin synthetic unbox method: *unbox-impl
                val unboxImplMethod = this.java.declaredMethods.firstOrNull {
                    it.name.endsWith("unbox-impl") && it.parameterCount == 0
                } ?: return@getOrPut null
                unboxImplMethod.isAccessible = true
                // Prefer MethodHandle to avoid reflective exceptions on hot path
                methodHandleLookup.unreflect(unboxImplMethod)
            } catch (_: Exception) {
                // Cache null as sentinel value; hot path won’t retry or throw
                null
            }
        }
    }

    /**
     * Underlying property of a **`value class`**.
     *
     * The underlying property might also be a `value class`!
     */
    private val <T : Any> KClass<T>.boxedProperty: KProperty1<T, *>
        get() =
            if (!this.isValue_safe) {
                throw UnsupportedOperationException("$this is not a value class")
            } else {
                @Suppress("UNCHECKED_CAST")
                valueClassFieldCache.getOrPut(this) {
                    // value classes always have exactly one property with a backing field
                    this.declaredMemberProperties.first { it.javaField != null }.apply { isAccessible = true }
                } as KProperty1<T, *>
            }

    /**
     * Returns `true` if calling [KClass.isValue] is safe.
     *
     * (In some instances [KClass.isValue] can throw an exception.)
     */
    private val <T : Any> KClass<T>.isValue_safe: Boolean
        get() =
            try {
                this.isValue
            } catch (_: KotlinReflectionInternalError) {
                false
            } catch (_: UnsupportedOperationException) {
                false
            } catch (_: AbstractMethodError) {
                false
            }
}
