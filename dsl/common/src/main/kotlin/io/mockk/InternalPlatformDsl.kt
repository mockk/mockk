package io.mockk

import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KCallable

expect object InternalPlatformDsl {
    fun identityHashCode(obj: Any): Int

    fun <T> runCoroutine(block: suspend () -> T): T

    fun Any?.toStr(): String

    fun deepEquals(obj1: Any?, obj2: Any?): Boolean

    fun unboxChar(value: Any): Any

    fun Any.toArray(): Array<*>

    fun classForName(name: String): Any

    fun dynamicCall(
        self: Any,
        methodName: String,
        args: Array<out Any?>,
        anyContinuationGen: () -> Continuation<*>
    ): Any?

    fun dynamicGet(self: Any, name: String): Any?

    fun dynamicSet(self: Any, name: String, value: Any?)

    fun dynamicSetField(self: Any, name: String, value: Any?)

    fun <T> threadLocal(initializer: () -> T): InternalRef<T>

    fun counter(): InternalCounter

    fun reflectionCall(
        callable: KCallable<*>,
        vararg params: Any?
    ): Any?
}

interface InternalRef<T> {
    val value: T
}


interface InternalCounter {
    val value: Long

    fun increment(): Long
}