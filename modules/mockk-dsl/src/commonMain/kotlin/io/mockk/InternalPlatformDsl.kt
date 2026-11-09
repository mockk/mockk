package io.mockk

import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

expect object InternalPlatformDsl {
    fun identityHashCode(obj: Any): Int

    fun <T> runCoroutine(block: suspend () -> T): T

    fun Any?.toStr(): String

    fun deepEquals(
        obj1: Any?,
        obj2: Any?,
    ): Boolean

    fun unboxChar(value: Any): Any

    fun Any.toArray(): Array<*>

    fun classForName(name: String): Any

    fun dynamicCall(
        self: Any,
        methodName: String,
        args: Array<out Any?>,
        anyContinuationGen: () -> Continuation<*>,
    ): Any?

    fun dynamicGet(
        self: Any,
        name: String,
    ): Any?

    fun dynamicSet(
        self: Any,
        name: String,
        value: Any?,
    )

    fun dynamicSetField(
        self: Any,
        name: String,
        value: Any?,
    )

    fun <T> threadLocal(initializer: () -> T): InternalRef<T>

    fun counter(): InternalCounter

    fun <T> coroutineCall(lambda: suspend () -> T): CoroutineCall<T>

    /**
     * Normally this simply casts [arg] to `T`
     *
     * However, if `T` is a `value class` (of type [cls]) this will construct a new instance of the
     * value class, and set [arg] as the value — but only when [arg] is not already an instance of
     * the value class, i.e. only when [arg] actually holds an unboxed value. An already boxed
     * argument is captured as-is instead of being boxed a second time.
     */
    internal fun <T : Any> boxCast(
        cls: KClass<*>,
        arg: Any,
    ): T
}

interface CoroutineCall<T> {
    fun callWithContinuation(continuation: Continuation<*>): T
}

interface InternalRef<T> {
    val value: T
}

interface InternalCounter {
    val value: Long

    fun increment(): Long
}
