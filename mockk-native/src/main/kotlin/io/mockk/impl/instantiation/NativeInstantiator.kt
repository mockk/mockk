package io.mockk.impl.instantiation

import kotlin.reflect.KClass

class NativeInstantiator(instanceFactoryRegistry: CommonInstanceFactoryRegistry) :
    AbstractInstantiator(instanceFactoryRegistry) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> instantiate(cls: KClass<T>): T {
        TODO("instantiate classw")
    }
}
