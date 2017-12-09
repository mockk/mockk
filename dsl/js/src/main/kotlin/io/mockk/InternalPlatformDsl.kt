package io.mockk

import kotlin.reflect.KClass

actual object InternalPlatformDsl {
    actual fun identityHashCode(obj: Any): Int = Kotlin.identityHashCode(obj)

    actual fun <T> runCoroutine(block: suspend () -> T): T =
            throw UnsupportedOperationException(
                    "Coroutines are not supported for JS MockK version")

    actual fun Any?.toStr(): String =
            when (this) {
                null -> "null"
                is KClass<*> -> this.simpleName ?: "<no name class>"
                is Function<*> -> "lambda {}"
                else -> toString()
            }

    actual fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
        return if (obj1 === obj2) {
            true
        } else if (obj1 == null || obj2 == null) {
            obj1 === obj2
        } else {
            arrayDeepEquals(obj1, obj2)
        }
    }

    private fun arrayDeepEquals(obj1: Any, obj2: Any): Boolean {
        return when (obj1) {
            is BooleanArray -> obj1 contentEquals obj2 as BooleanArray
            is ByteArray -> obj1 contentEquals obj2 as ByteArray
            is CharArray -> obj1 contentEquals obj2 as CharArray
            is ShortArray -> obj1 contentEquals obj2 as ShortArray
            is IntArray -> obj1 contentEquals obj2 as IntArray
            is LongArray -> obj1 contentEquals obj2 as LongArray
            is FloatArray -> obj1 contentEquals obj2 as FloatArray
            is DoubleArray -> obj1 contentEquals obj2 as DoubleArray
            is Array<*> -> return obj1 contentDeepEquals obj2 as Array<*>
            else -> obj1 == obj2
        }
    }

    actual fun unboxChar(value: Any): Any =
            if (value is Char) {
                value.toInt()
            } else {
                value
            }
}

internal external object Kotlin {
    fun identityHashCode(obj: Any): Int
}
