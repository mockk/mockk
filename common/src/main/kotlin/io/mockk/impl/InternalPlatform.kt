package io.mockk.impl

import io.mockk.Ref
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

    fun counter(): () -> Long

    fun packRef(arg: Any?): Any?

    fun isSuspend(paramTypes: List<KClass<Any>>): Boolean

    fun prettifyRecordingException(ex: Throwable): Throwable

    fun <T : Any> copyFields(to: T, from: T)

    fun captureStackTrace(): List<StackElement>
}
