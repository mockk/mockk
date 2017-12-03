package io.mockk.impl

import io.mockk.InternalPlatform.toStr
import io.mockk.proxy.MockKProxyMaker
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

internal class InstantiatorImpl(val proxyMaker: MockKProxyMaker,
                                val factoryRegistry: InstanceFactoryRegistryImpl) : Instantiator {

    override fun <T : Any> instantiate(cls: KClass<T>): T {
        log.trace { "Building empty instance ${cls.toStr()}" }

        for (factory in factoryRegistry.instanceFactories) {
            val instance = factory.instantiate(cls)
            if (instance != null) {
                log.trace { "Instance factory returned instance $instance" }
                return cls.cast(instance)
            }
        }

        return proxyMaker.instance(cls.java)
    }

    companion object {
        val log = Logger<InstantiatorImpl>()
    }
}
