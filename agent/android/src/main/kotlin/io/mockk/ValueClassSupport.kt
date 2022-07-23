package io.mockk

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Underlying property value of a **`value class`** or self
 */
val <T : Any> T.boxedValue: Any?
    @Suppress("UNCHECKED_CAST")
    get() = if (!this::class.isValue_safe) this
    else (this::class as KClass<T>).boxedProperty.get(this)

/**
 * Underlying property class of a **`value class`** or self
 */
val KClass<*>.boxedClass: KClass<*>
    get() = if (!this.isValue_safe) this
    else this.boxedProperty.returnType.classifier as KClass<*>

/**
 * Underlying property of a **`value class`**
 */
private val <T : Any> KClass<T>.boxedProperty: KProperty1<T, *>
    get() = if (!this.isValue_safe) throw UnsupportedOperationException("$this is not a value class")
    // value classes always have exactly one property
    else this.declaredMemberProperties.first().apply { isAccessible = true }

private val <T : Any> KClass<T>.isValue_safe: Boolean
    get() = try {
        this.isValue
    } catch (_: UnsupportedOperationException) {
        false
    }
