package io.mockk.proxy

interface MockKProxyMaker {
    fun <T : Any> proxy(
        clazz: Class<T>,
        interfaces: Array<Class<*>>,
        handler: MockKInvocationHandler,
        useDefaultConstructor: Boolean,
        instance: Any?
    ): Cancelable<T>
}
