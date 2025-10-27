package io.mockk.core

import io.mockk.core.ValueClassSupport.boxedValue
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

actual object ValueClassSupport {
    private val unboxValueReturnTypes = setOf(Result.success("").javaClass.kotlin)

    /**
     * Unboxes the underlying property value of a **`value class`** or self, as long the unboxed value is appropriate
     * for the given method's return type.
     *
     * @see boxedValue
     */
    actual fun <T : Any> T.maybeUnboxValueForMethodReturn(method: Method): Any? {
        val resultType = this::class
        // Don't unbox if not a value class
        if (!resultType.isValue_safe) return this

        // Unbox Kotlin synthetic unbox methods
        if (method.name.endsWith("unbox-impl")) return this.boxedValue

        val kFunction = method.kotlinFunction
        val kProperty = if (kFunction == null) findMatchingPropertyWithJavaGetter(method) else null

        val expectedReturnType = when {
            kFunction != null -> kFunction.returnType.classifier
            kProperty != null -> kProperty.returnType.classifier
            else -> return this
        }

        // For generic type parameters, safely check if the underlying value is null.
        // Avoid accessing properties on mock value classes (which would trigger unstubbed getters).
        if (expectedReturnType !is KClass<*>) {
            try {
                val unbox = this.javaClass.methods.firstOrNull {
                    it.name.endsWith("unbox-impl") && it.parameterCount == 0
                }
                if (unbox != null) {
                    unbox.isAccessible = true
                    unbox.invoke(this) ?: return null
                }
            } catch (_: Throwable) {
                // fall through and return the instance as-is
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
            // Don't unbox when returning via supertype/interface.
            isExpectedTypeSupertype -> this
            // Don't unbox if nullable primitive or suspend fun with primitive.
            isExpectedTypeValueClass && (isReturnNullable && isPrimitive) -> this
            isExpectedTypeValueClass && (kFunction?.isSuspend == true && isPrimitive) -> this
            // Unbox for value class return type.
            isExpectedTypeValueClass -> this.boxedValue
            // For property: unbox if not nullable primitive.
            kProperty != null && !(isReturnNullable && isPrimitive) -> this.boxedValue
            // Default: don't unbox.
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
