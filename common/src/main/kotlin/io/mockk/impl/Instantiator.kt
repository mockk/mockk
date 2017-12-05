package io.mockk.impl

import kotlin.reflect.KClass

abstract class Instantiator(val instanceFactoryRegistry: InstanceFactoryRegistryImpl) {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> instantiateViaInstanceFactoryRegistry(cls: KClass<T>, orInstantiate: () -> T): T {
        for (factory in instanceFactoryRegistry.instanceFactories) {
            val instance = factory.instantiate(cls)
            if (instance != null) {
                log.trace { "Instance factory returned instance $instance" }
                return instance as T
            }
        }
        return orInstantiate()
    }

    abstract fun <T : Any> instantiate(cls: KClass<T>): T

    companion object {
        val log = Logger<Instantiator>()
    }
}