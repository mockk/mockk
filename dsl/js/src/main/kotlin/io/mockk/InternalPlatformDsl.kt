package io.mockk

import kotlin.coroutines.Continuation
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

actual object InternalPlatformDsl {
    actual fun identityHashCode(obj: Any): Int = Kotlin.identityHashCode(obj)

    actual fun <T> runCoroutine(block: suspend () -> T): T =
        throw UnsupportedOperationException(
            "Coroutines are not supported for JS MockK version"
        )

    actual fun Any?.toStr(): String =
        try {
            when (this) {
                null -> "null"
                is BooleanArray -> this.contentToString()
                is ByteArray -> this.contentToString()
                is CharArray -> this.contentToString()
                is ShortArray -> this.contentToString()
                is IntArray -> this.contentToString()
                is LongArray -> this.contentToString()
                is FloatArray -> this.contentToString()
                is DoubleArray -> this.contentToString()
                is Array<*> -> this.contentDeepToString()
                is KClass<*> -> this.simpleName ?: "<null name class>"
                is Function<*> -> "lambda {}"
                else -> toString()
            }
        } catch (thr: Throwable) {
            "<error \"$thr\">"
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

    actual fun Any.toArray(): Array<*> =
        when (this) {
            is BooleanArray -> this.toTypedArray()
            is ByteArray -> this.toTypedArray()
            is CharArray -> this.toTypedArray()
            is ShortArray -> this.toTypedArray()
            is IntArray -> this.toTypedArray()
            is LongArray -> this.toTypedArray()
            is FloatArray -> this.toTypedArray()
            is DoubleArray -> this.toTypedArray()
            else -> this as Array<*>
        }

    actual fun classForName(name: String): Any = throw MockKException("classForName is not support on JS platform")

    actual fun dynamicCall(
        self: Any,
        methodName: String,
        args: Array<out Any?>,
        anyContinuationGen: () -> Continuation<*>
    ): Any? = throw MockKException("dynamic call is not supported on JS platform")

    actual fun dynamicGet(self: Any, name: String): Any? =
        throw MockKException("dynamic get is not supported on JS platform")

    actual fun dynamicSet(self: Any, name: String, value: Any?) {
        throw MockKException("dynamic set is not supported on JS platform")
    }

    actual fun dynamicSetField(self: Any, name: String, value: Any?) {
        throw MockKException("dynamic set is not supported on JS platform")
    }

    actual fun <T> threadLocal(initializer: () -> T): InternalRef<T> {
        return object : InternalRef<T> {
            override val value = initializer()
        }
    }

    actual fun counter() = object : InternalCounter {
        override var value = 0L

        override fun increment() = value++
    }

    actual fun <T> coroutineCall(lambda: suspend () -> T): CoroutineCall<T> {
        throw MockKException("coroutineCall is not supported")
    }
}

internal external object Kotlin {
    fun identityHashCode(obj: Any): Int
}
