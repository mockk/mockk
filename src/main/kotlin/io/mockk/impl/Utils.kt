package io.mockk.impl

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


internal class Ref(val value: Any) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ref

        if (value !== other.value) return false

        return true
    }

    override fun hashCode(): Int = System.identityHashCode(value)
    override fun toString(): String = "Ref(${value.javaClass.simpleName}@${hashCode()})"
}

internal fun InvocationTargetException.demangle(): Throwable {
    var ex: Throwable = this
    while (ex.cause != null &&
            ex is InvocationTargetException) {
        ex = ex.cause!!
    }
    throw ex
}

internal fun Any?.toStr() =
        when (this) {
            null -> "null"
            is Method -> name + "(" + parameterTypes.map { it.simpleName }.joinToString() + ")"
            else -> toString()
        }

internal fun Method.parameterCount(): Int = parameterTypes.size

internal fun <T> threadLocalOf(init: () -> T): ThreadLocal<T> {
    return object : ThreadLocal<T>() {
        override fun initialValue(): T = init.invoke()
    }
}
