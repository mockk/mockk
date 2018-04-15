package io.mockk.impl.interception

import kotlin.reflect.KClass

interface InterceptorMap {
    fun execute(interceptor: Interceptor, name: String, vararg types: KClass<*>): Interceptor
}

typealias InterceptorMappingBlock = InterceptorMap.() -> Unit

object Interceptors {
    internal val interceptors = mutableMapOf<KClass<*>, InterceptorMappingBlock>()

    fun register(cls: KClass<*>, mappingBlock: InterceptorMappingBlock) {
        interceptors.put(cls, mappingBlock)
    }
}
