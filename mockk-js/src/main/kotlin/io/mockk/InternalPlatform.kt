package io.mockk

import io.mockk.js.JsCounter
import io.mockk.js.JsIdentityHashMapOf
import io.mockk.js.JsRef
import kotlin.js.Date
import kotlin.reflect.KClass

actual object InternalPlatform {
    actual fun nanoTime() = (Date().getTime() * 1e6).toLong()

    actual fun ref(obj: Any): Ref = JsRef(obj)

    actual fun hkd(obj: Any): String = InternalPlatformDsl.identityHashCode(obj).toString()

    actual fun isPassedByValue(cls: KClass<*>): Boolean {
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

    actual fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V {
        val value = get(key)
        return if (value == null) {
            val newValue = valueFunc(key)
            put(key, newValue)
            newValue
        } else {
            value
        }
    }

    actual fun counter(): () -> Long = JsCounter()::next

    actual fun packRef(arg: Any?): Any? {
        return if (arg == null || isPassedByValue(arg::class))
            arg
        else
            ref(arg)
    }

    actual fun isSuspend(paramTypes: List<KClass<Any>>): Boolean {
        return false
    }

    actual fun prettifyRecordingException(ex: Throwable) = ex

    actual fun <K : Any, V> weakMap(): MutableMap<K, V> = JsIdentityHashMapOf()

    actual fun <T> synchronizedMutableList(): MutableList<T> = mutableListOf()

    actual fun <K, V> synchronizedMutableMap(): MutableMap<K, V> = hashMapOf()

    @Suppress("NAME_SHADOWING", "UNUSED_VARIABLE")
    actual fun <T> copyFields(to: T, from: T) {
        val to = to.asDynamic()
        val from = from.asDynamic()
        js("for (var key in from) { to[key] = from[key]; }")
    }
}

