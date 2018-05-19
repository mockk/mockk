package io.mockk.proxy.jvm.transformation

internal data class CacheKey(
    val clazz: Class<*>,
    val interfaces: Set<Class<*>>
)
