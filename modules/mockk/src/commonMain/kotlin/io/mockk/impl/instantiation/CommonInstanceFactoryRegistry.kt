package io.mockk.impl.instantiation

import io.mockk.MockKGateway.InstanceFactory
import io.mockk.MockKGateway.InstanceFactoryRegistry
import io.mockk.impl.InternalPlatform

class CommonInstanceFactoryRegistry : InstanceFactoryRegistry {
    private val factories = InternalPlatform.synchronizedMutableList<InstanceFactory>()

    val instanceFactories: List<InstanceFactory>
        get() = InternalPlatform.synchronized(factories) {
            factories.toList()
        }

    override fun registerFactory(factory: InstanceFactory) {
        factories.add(factory)
    }

    override fun deregisterFactory(factory: InstanceFactory) {
        factories.remove(factory)
    }
}
