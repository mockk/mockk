package io.mockk.proxy.common.transformation

interface SubclassInstrumentation {
    fun <T> subclass(
        clazz: Class<T>,
        interfaces: Array<Class<*>>
    ): Class<T>
}
