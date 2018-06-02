package io.mockk.proxy

interface MockKInstantiatior {
    fun <T> instance(cls: Class<T>): T
}
