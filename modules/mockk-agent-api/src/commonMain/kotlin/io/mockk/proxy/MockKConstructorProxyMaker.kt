package io.mockk.proxy

interface MockKConstructorProxyMaker {
    fun constructorProxy(
        clazz: Class<*>,
        handler: MockKInvocationHandler
    ): Cancelable<Class<*>>
}
