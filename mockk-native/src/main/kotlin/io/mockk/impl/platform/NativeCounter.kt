package io.mockk.impl.platform

internal class NativeCounter {
    var id: Long = 1
    fun next() = id++
}