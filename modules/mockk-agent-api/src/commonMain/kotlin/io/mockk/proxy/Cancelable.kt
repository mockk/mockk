package io.mockk.proxy

interface Cancelable<T> {
    fun get(): T

    fun cancel()
}
