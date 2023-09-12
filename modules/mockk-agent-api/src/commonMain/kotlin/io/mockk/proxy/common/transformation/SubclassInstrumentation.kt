package io.mockk.proxy.common.transformation

import io.mockk.proxy.MockKInvocationHandler

interface SubclassInstrumentation {
    fun <T> subclass(
        clazz: Class<T>,
        interfaces: Array<Class<*>>
    ): Class<T>

    fun setProxyHandler(proxy: Any, handler: MockKInvocationHandler)
}
