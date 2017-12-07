package io.mockk

import io.mockk.js.JsCounter
import io.mockk.js.JsIdentityHashMapOf
import io.mockk.js.JsRef
import kotlin.js.Date
import kotlin.reflect.KClass

object InternalPlatform {
    fun nanoTime() = (Date().getTime() * 1e6).toLong()

    fun ref(obj: Any): Ref = JsRef(obj)

    fun <T> runCoroutine(block: suspend () -> T): T =
            throw UnsupportedOperationException(
                    "Coroutines are not supported for JS MockK version")

    fun Any?.toStr(): String =
            when (this) {
                null -> "null"
                is KClass<*> -> this.simpleName ?: "<no name class>"
                is Function<*> -> "lambda {}"
                else -> toString()
            }

    fun hkd(obj: Any): String = identityHashCode(obj).toString()

    fun isPassedByValue(cls: KClass<*>): Boolean {
        return when (cls) {
            Boolean::class -> true
            Byte::class -> true
            Short::class -> true
            Char::class -> true
            Int::class -> true
            Long::class -> true
            Float::class -> true
            Double::class -> true
            String::class -> true
            else -> false
        }
    }

    fun deepEquals(obj1: Any?, obj2: Any?): Boolean {
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

    fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V {
        val value = get(key)
        return if (value == null) {
            val newValue = valueFunc(key)
            put(key, newValue)
            newValue
        } else {
            value
        }
    }

    fun counter(): () -> Long = JsCounter()::next

    fun packRef(arg: Any?): Any? {
        return if (arg == null || isPassedByValue(arg::class))
            arg
        else
            ref(arg)
    }

    fun isSuspend(paramTypes: List<KClass<*>>): Boolean {
        return false
    }

    fun prettifyRecordingException(ex: Throwable) = ex

    fun <K : Any, V> weakMap(): MutableMap<K, V> = JsIdentityHashMapOf()

    fun <T> synchronizedMutableList(): MutableList<T> = mutableListOf()

    fun <K, V> synchronizedMutableMap(): MutableMap<K, V> = hashMapOf()

    fun <T> copyFields(to: T, from: T) {
        val to = to.asDynamic()
        val from = from.asDynamic()
        js("for (var key in from) { to[key] = from[key]; }")
    }
}

