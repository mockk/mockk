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
 * JVM-specific helpers for value class support.
 */
actual object ValueClassSupport {
    private val unboxValueReturnTypes = setOf(Result.success("").javaClass.kotlin)

    /**
     * Unboxes the underlying property value of a **`value class`** or self, as long the unboxed value is appropriate
     * for the given method's return type.
     *
     * @return The original instance, its underlying value, or `null` for logical nulls in generic contexts.
     * @see boxedValue
     */
    actual fun <T : Any> T.maybeUnboxValueForMethodReturn(method: Method): Any? {
        val resultType = this::class
        // Don't unbox if not a value class
        if (!resultType.isValue_safe) return this

        // Unbox Kotlin synthetic *unbox-impl methods
        if (method.name.endsWith("unbox-impl")) return this.boxedValue

        val kFunction = method.kotlinFunction
        // It is possible that the method is a getter for a property, in which
        // case we can check the property's return type in kotlin
        val kProperty = if (kFunction == null) findMatchingPropertyWithJavaGetter(method) else null

        val expectedReturnType =
            when {
                kFunction != null -> kFunction.returnType.classifier
                kProperty != null -> kProperty.returnType.classifier
                else -> return this
            }

        // For generic return types, use a cached MethodHandle to probe for null.
        // This avoids calling mocked/intercepted property getters.
        if (expectedReturnType !is KClass<*>) {
            val handle = resultType.resolveUnboxHandleOrNull()
            if (handle != null) {
                handle.invoke(this) ?: return null
            }
            return this
        }

        val isReturnNullable =
            when {
                kFunction != null -> kFunction.returnType.isMarkedNullable
                kProperty != null -> kProperty.returnType.isMarkedNullable
                else -> false
            }

        // Use innermostBoxedClass with recursion limit to avoid infinite loops
        // (issue #1103) while still handling nested value classes (issue #1308)
        val isPrimitive = resultType.innermostBoxedClass().java.isPrimitive
        val isExpectedTypeValueClass = expectedReturnType == resultType
        val isExpectedTypeSupertype =
            expectedReturnType != resultType &&
                expectedReturnType.isSuperclassOf(resultType)

        return when {
            // Don't unbox when returning via supertype or interface
            isExpectedTypeSupertype -> this
            // Don't unbox for nullable primitives or suspend functions returning primitives
            isExpectedTypeValueClass && (isReturnNullable && isPrimitive) -> this
            isExpectedTypeValueClass && (kFunction?.isSuspend == true && isPrimitive) -> this
            // Unbox when returning the value class type directly
            isExpectedTypeValueClass -> this.boxedValue
            // Unbox for properties unless it's a nullable primitive
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
     * Cached [MethodHandle]s to synthetic `unbox-impl` methods.
     */
    private val valueClassUnboxHandleCache = mutableMapOf<KClass<out Any>, MethodHandle?>()

    /**
     * Lookup used to unreflect the discovered method into a MethodHandle (lazily created).
     */
    private val methodHandleLookup by lazy { MethodHandles.lookup() }

    /**
     * Resolves a [MethodHandle] for the `unbox-impl` method of a value class.
     */
    private fun <T : Any> KClass<T>.resolveUnboxHandleOrNull(): MethodHandle? {
        // There's no such method for non-value classes
        if (!this.isValue_safe) return null

        return valueClassUnboxHandleCache.getOrPut(this) {
            try {
                // Find zero-arg Kotlin synthetic unbox method: *unbox-impl
                val unboxImplMethod =
                    this.java.declaredMethods.firstOrNull {
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
