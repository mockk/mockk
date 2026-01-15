package io.mockk.impl.annotations

import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

internal object InjectionHelpers {
    @Suppress("UNCHECKED_CAST")
    fun KProperty1<*, *>.getAny(obj: Any): Any? {
        try {
            isAccessible = true
        } catch (ex: Throwable) {
            // skip
        }
        return (this.getter as KProperty1.Getter<Any, Any?>)(obj)
    }

    fun KProperty1<*, *>.getAnyIfLateNull(obj: Any): Any? {
        try {
            return getAny(obj)
        } catch (ex: InvocationTargetException) {
            if (isLateinit &&
                ex.cause != null &&
                ex.cause is UninitializedPropertyAccessException
            ) {
                return null
            }
            throw ex
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun KMutableProperty1<*, *>.setAny(
        obj: Any,
        value: Any?,
    ) {
        try {
            isAccessible = true
        } catch (ex: Throwable) {
            // skip
        }
        return (this.setter as KMutableProperty1.Setter<Any, Any?>).invoke(obj, value)
    }

    fun KProperty1<*, *>.setImmutableAny(
        obj: Any,
        value: Any?,
    ) {
        val javaField = this.javaField ?: return
        try {
            javaField.isAccessible = true
        } catch (ex: Throwable) {
            // skip
        }
        javaField.set(obj, value)
    }

    fun KProperty<*>.getReturnTypeKClass(): KClass<*>? = returnType.classifier as? KClass<*>

    fun KType.getKClass(): KClass<*>? = classifier as? KClass<*>

    fun KClass<*>.getConstructorParameterTypes(): Set<KClass<*>> =
        constructors
            .flatMap { constructor ->
                constructor.parameters.mapNotNull { it.type.getKClass() }
            }.toSet()
}
