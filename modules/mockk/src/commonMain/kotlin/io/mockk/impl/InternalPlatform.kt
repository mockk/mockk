package io.mockk.impl

import io.mockk.StackElement
import kotlin.reflect.KClass

expect object InternalPlatform {
    fun time(): Long

    fun ref(obj: Any): Ref

    fun hkd(obj: Any): String

    fun isPassedByValue(cls: KClass<*>): Boolean

    fun <K, V> MutableMap<K, V>.customComputeIfAbsent(key: K, valueFunc: (K) -> V): V

    fun <K, V> weakMap(): MutableMap<K, V>

    fun <K, V> identityMap(): MutableMap<K, V>

    fun <T> synchronizedMutableList(): MutableList<T>

    fun <K, V> synchronizedMutableMap(): MutableMap<K, V>

    fun packRef(arg: Any?): Any?

    fun prettifyRecordingException(ex: Throwable): Throwable

    fun <T : Any> copyFields(to: T, from: T)

    fun captureStackTrace(): () -> List<StackElement>

    fun weakRef(value: Any): WeakRef

    fun multiNotifier(): MultiNotifier

    inline fun <T> synchronized(obj: Any, block: () -> T): T
}

interface Ref {
    val value: Any
}

interface WeakRef {
    val value: Any?
}

interface MultiNotifier {
    fun notify(key: Any)

    fun openSession(keys: List<Any>, timeout: Long): Session

    interface Session {
        fun wait(): Boolean

        fun close()
    }
}
