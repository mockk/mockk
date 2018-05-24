package io.mockk.proxy

interface MockKStaticProxyMaker {
    fun staticProxy(
        clazz: Class<*>,
        handler: MockKInvocationHandler
    ): Cancelable<Class<*>>
}
