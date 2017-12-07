package io.mockk.js

internal class JsCounter {
    var id: Long = 1
    fun next() = id++
}