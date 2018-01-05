package io.mockk

expect object InternalPlatformDsl {
    fun identityHashCode(obj: Any): Int

    fun <T> runCoroutine(block: suspend () -> T): T

    fun Any?.toStr(): String

    fun deepEquals(obj1: Any?, obj2: Any?): Boolean

    fun unboxChar(value: Any): Any

    fun Any.toArray(): Array<*>
}
