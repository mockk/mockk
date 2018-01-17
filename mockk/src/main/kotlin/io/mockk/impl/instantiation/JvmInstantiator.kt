package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.impl.log.Logger
import io.mockk.proxy.MockKProxyMaker
import kotlin.reflect.KClass

class JvmInstantiator(
    val proxyMaker: MockKProxyMaker,
    instanceFactoryRegistry: CommonInstanceFactoryRegistry
) : AbstractInstantiator(instanceFactoryRegistry) {

    override fun <T : Any> instantiate(cls: KClass<T>): T {
        log.trace { "Building empty instance ${cls.toStr()}" }
        return instantiateViaInstanceFactoryRegistry(cls) {
            proxyMaker.instance(cls.java)
        }
    }

    companion object {
        val log = Logger<JvmInstantiator>()
    }
}
