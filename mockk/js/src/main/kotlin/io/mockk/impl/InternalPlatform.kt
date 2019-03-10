package io.mockk.impl

import io.mockk.InternalPlatformDsl
import io.mockk.StackElement
import io.mockk.impl.platform.CommonIdentityHashMapOf
import io.mockk.impl.platform.CommonRef
import io.mockk.impl.platform.JsCounter
import io.mockk.impl.platform.JsHexLongHelper
import kotlin.reflect.KClass

actual object InternalPlatform {
    internal val timeCounter = JsCounter()

    actual fun time() = timeCounter.next()

    actual fun ref(obj: Any): Ref = CommonRef(obj)

    actual fun hkd(obj: Any): String = JsHexLongHelper.toHexString(InternalPlatformDsl.identityHashCode(obj).toLong())

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

    actual fun packRef(arg: Any?): Any? {
        return if (arg == null || isPassedByValue(arg::class))
            arg
        else
            ref(arg)
    }

    actual fun prettifyRecordingException(ex: Throwable) = ex

    actual fun <K, V> weakMap(): MutableMap<K, V> = CommonIdentityHashMapOf()

    actual fun <K, V> identityMap(): MutableMap<K, V> = CommonIdentityHashMapOf()

    actual fun <T> synchronizedMutableList(): MutableList<T> = mutableListOf()

    actual fun <K, V> synchronizedMutableMap(): MutableMap<K, V> = hashMapOf()

    @Suppress("NAME_SHADOWING", "UNUSED_VARIABLE")
    actual fun <T : Any> copyFields(to: T, from: T) {
        val to = to.asDynamic()
        val from = from.asDynamic()
        js("for (var key in from) { to[key] = from[key]; }")
    }

    // TODO
    actual fun captureStackTrace() = { listOf<StackElement>() }

    actual fun weakRef(value: Any) = object : WeakRef {
        override val value: Any?
            get() = value
    }

    actual fun multiNotifier() = object : MultiNotifier {
        override fun notify(key: Any) {
            // skip
        }

        override fun openSession(keys: List<Any>, timeout: Long) =
            throw UnsupportedOperationException("not implemented for JS")
    }
}

