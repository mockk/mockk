package io.mockk.impl

import kotlin.reflect.KClass

class JsInstantiator(instanceFactoryRegistry: InstanceFactoryRegistryImpl) : Instantiator(instanceFactoryRegistry) {
    override fun <T : Any> instantiate(cls: KClass<T>): T {
        return instantiateViaInstanceFactoryRegistry(cls) {
            Proxy(InstanceStubTarget(), InstanceProxyHandler()) as T
        }
    }
}

internal class InstanceStubTarget  {
    override fun toString() = "<instance>"

}

internal class InstanceProxyHandler : EmptyProxyHandler() {
    override fun get(target: dynamic, name: String, receiver: dynamic): Any {
        if (isJsNativeMethods(name)) {
            return target[name]
        }
        return super.get(target, name, receiver)
    }
}