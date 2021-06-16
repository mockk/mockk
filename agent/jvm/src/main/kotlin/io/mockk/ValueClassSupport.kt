package io.mockk.proxy.jvm

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

private val valueClassFieldCache = mutableMapOf<KClass<out Any>, KProperty1<out Any, *>>()

/**
 * Get boxed value of any value class
 *
 * @return boxed value of value class, if this is value class, else just itself
 */
fun <T : Any> T.boxedValue(): Any? {
    if (!this::class.isValueClass()) return this

    // get backing field
    val backingField = this::class.valueField()

    // get boxed value
    @Suppress("UNCHECKED_CAST")
    return (backingField as KProperty1<T, *>).get(this)
}

/**
 * Get class of boxed value of any value class
 *
 * @return class of boxed value, if this is value class, else just class of itself
 */
fun <T : Any> T.boxedClass(): KClass<*> {
    if (!this::class.isValueClass()) return this::class

    // get backing field
    val backingField = this::class.valueField()

    // get boxed value
    return backingField.returnType.classifier as KClass<*>
}


private fun <T : Any> KClass<T>.valueField(): KProperty1<out T, *> {
    @Suppress("UNCHECKED_CAST")
    return valueClassFieldCache.getOrPut(this) {
        require(isValue) { "$this is not a value class" }

        // value classes always have a primary constructor...
        val constructor = primaryConstructor!!
        // ...and exactly one constructor parameter
        val constructorParameter = constructor.parameters.first()
        // ...with a backing field
        val backingField = declaredMemberProperties
            .first { it.name == constructorParameter.name }
            .apply { isAccessible = true }

        backingField
    } as KProperty1<out T, *>
}

private fun <T : Any> KClass<T>.isValueClass() = try {
    this.isValue
} catch (_: UnsupportedOperationException) {
    false
}

/**
 * POLYFILL for kotlin version < 1.5
 * will be shadowed by implementation in kotlin SDK 1.5+
 *
 * @return true if this is an inline class, else false
 */
private val <T : Any> KClass<T>.isValue: Boolean
    get() = !isData &&
            primaryConstructor?.parameters?.size == 1 &&
            java.declaredMethods.any { it.name == "box-impl" }
