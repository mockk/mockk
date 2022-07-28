package io.mockk.impl.platform

internal class JsCounter {
    var id: Long = 1
    fun next() = id++
}