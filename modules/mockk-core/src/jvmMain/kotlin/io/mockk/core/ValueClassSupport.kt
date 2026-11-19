package io.mockk.core

import io.mockk.core.ValueClassSupport.boxedValue
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeParameter
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

        val kFunction = method.kotlinFunctionOrNull()
        // It is possible that the method is a getter for a property, in which
        // case we can check the property's return type in kotlin
        val kProperty = if (kFunction == null) findMatchingPropertyWithJavaGetter(method) else null

        val expectedReturnType =
            when {
                kFunction != null -> kFunction.returnType.classifier
                kProperty != null -> kProperty.returnType.classifier
                else -> return this
            }

        // For generic return types, use a cached Method to probe for null.
        // This avoids calling mocked/intercepted property getters.
        if (expectedReturnType !is KClass<*>) {
            val method = resultType.resolveUnboxMethodOrNull()
            if (method != null) {
                method.invoke(this) ?: return null
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

    /**
     * Resolves the Kotlin function behind a Java [Method], or `null` when kotlin-reflect
     * cannot map it.
     *
     * The synthetic `invoke` of function/SAM types has no mappable descriptor:
     * [kotlinFunction] throws [KotlinReflectionInternalError] with "Unknown origin ..."
     * for members declared in `kotlin.Function0` and friends (issues #1242). Unboxing
     * only needs the descriptor to decide *how* to return a value class, so `null`
     * safely means "pass the value through unchanged".
     */
    private fun Method.kotlinFunctionOrNull(): KFunction<*>? =
        try {
            kotlinFunction
        } catch (_: KotlinReflectionInternalError) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        } catch (_: AbstractMethodError) {
            null
        }

    private fun findMatchingPropertyWithJavaGetter(method: Method): KProperty<*>? =
        try {
            method.declaringClass.kotlin.declaredMemberProperties.find {
                it.javaGetter == method
            }
        } catch (_: KotlinReflectionInternalError) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        } catch (_: AbstractMethodError) {
            null
        }

    /**
     * Re-boxes the JVM-erased result of a method invocation into the value-class instance
     * the method's Kotlin signature promises.
     *
     * The Kotlin compiler erases a non-nullable value-class return to its underlying type and
     * mangles the method name (`charge-xQ1N4Yw(int): int`), so reflectively invoking the
     * original implementation yields the raw underlying value (e.g. an `Integer`) where the
     * declared Kotlin return type is the value class (`Amount`). Consumers that deliver the
     * result as the declared type - answer-scope `callOriginal()` - need it boxed back first.
     *
     * Results that already are instances of the declared type pass through unchanged, as do
     * suspend calls: their results arrive boxed through the continuation machinery, and a
     * `COROUTINE_SUSPENDED` marker must not be treated as a value. Methods whose descriptor
     * cannot be mapped to a Kotlin declaration (see [kotlinFunctionOrNull]) pass through
     * unchanged as well.
     *
     * @return The boxed value-class instance, or [erasedResult] unchanged when no re-boxing applies.
     * @see maybeUnboxValueForMethodReturn
     */
    fun maybeBoxValueForMethodReturn(
        method: Method,
        erasedResult: Any?,
    ): Any? {
        if (erasedResult == null) return erasedResult

        // A suspending original delivers its result (or the COROUTINE_SUSPENDED marker) through
        // the continuation machinery, which already handles the value-class shape.
        if (method.parameterCount > 0 &&
            Continuation::class.java.isAssignableFrom(method.parameterTypes.last())
        ) {
            return erasedResult
        }

        val declaredReturnType = method.declaredReturnTypeOrNull() ?: return erasedResult
        if (!declaredReturnType.isValue_safe) return erasedResult
        if (declaredReturnType.isInstance(erasedResult)) return erasedResult
        return erasedResult.boxAs(declaredReturnType)
    }

    /**
     * Declared Kotlin return type of a [Method], or `null` when it cannot be determined
     * (plain Java methods, generic type parameters, unmappable descriptors).
     */
    private fun Method.declaredReturnTypeOrNull(): KClass<*>? =
        try {
            val kFunction = kotlinFunctionOrNull()
            val kProperty = if (kFunction == null) findMatchingPropertyWithJavaGetter(this) else null
            when {
                kFunction != null -> kFunction.returnType.classifier as? KClass<*>
                kProperty != null -> kProperty.returnType.classifier as? KClass<*>
                else -> null
            }
        } catch (_: KotlinReflectionInternalError) {
            null
        } catch (_: UnsupportedOperationException) {
            null
        } catch (_: AbstractMethodError) {
            null
        }

    /**
     * Boxes [this] underlying value into the [klass] value-class instance.
     *
     * The synthetic `box-impl` method declares the storage representation the compiler
     * flattened the chain to: for a value class backed by another value class
     * (e.g. `Quantity(Amount)`) it takes the innermost raw value (`Quantity.box-impl(int)`),
     * for a boxed-form property it takes the boxed inner instance. The declared parameter
     * type decides what is passed, so the raw value is forwarded whenever it already matches
     * and only boxed one level down when it does not.
     *
     * @return The boxed instance, or the receiver unchanged when no `box-impl` can be invoked.
     */
    private fun Any?.boxAs(
        klass: KClass<*>,
        depth: Int = 10,
    ): Any? {
        if (!klass.isValue_safe) return this
        if (depth <= 0) return this
        val boxMethod =
            klass.java.declaredMethods.firstOrNull {
                it.name == "box-impl" && it.parameterCount == 1
            } ?: return this
        val paramType = boxMethod.parameterTypes[0]
        val argument =
            when {
                paramType.isPrimitive || paramType.isInstance(this) -> this
                else -> boxAs(klass.boxedClass, depth - 1)
            }
        return runCatching {
            boxMethod.isAccessible = true
            boxMethod.invoke(null, argument)
        }.getOrDefault(this)
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
                this.boxedProperty.returnType.classifier.let { classifier ->
                    when (classifier) {
                        is KClass<*> -> classifier
                        is KTypeParameter -> classifier.upperBounds.firstNotNullOfOrNull { it.classifier as? KClass<*> } ?: Any::class
                        else -> Any::class
                    }
                }
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
     * Cached [Method]s to synthetic `unbox-impl` methods.
     */
    private val valueClassUnboxMethodCache = mutableMapOf<KClass<out Any>, Method?>()

    /**
     * Resolves a [Method] for the `unbox-impl` method of a value class.
     */
    private fun <T : Any> KClass<T>.resolveUnboxMethodOrNull(): Method? {
        // There's no such method for non-value classes
        if (!this.isValue_safe) return null

        return valueClassUnboxMethodCache.getOrPut(this) {
            try {
                // Find zero-arg Kotlin synthetic unbox method: *unbox-impl
                val unboxImplMethod =
                    this.java.declaredMethods.firstOrNull {
                        it.name.endsWith("unbox-impl") && it.parameterCount == 0
                    } ?: return@getOrPut null
                unboxImplMethod.isAccessible = true
                unboxImplMethod
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
