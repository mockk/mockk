package io.mockk.proxy.android

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.MockKInstantiatior
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Proxy
import java.util.*

internal class OnjenesisInstantiator(val log: MockKAgentLogger) : MockKInstantiatior {
    private val objenesis = ObjenesisStd(true)
    private val instantiators = Collections.synchronizedMap(WeakHashMap<Class<*>, ObjectInstantiator<*>>())

    override fun <T> instance(cls: Class<T>): T {
        val clazz = proxyInterface(cls)

        log.trace("Creating new empty instance of $clazz")

        val inst = instantiators.getOrPut(clazz, {
            objenesis.getInstantiatorOf(clazz)
        })

        return clazz.cast(inst.newInstance())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxyInterface(clazz: Class<T>) =
        if (clazz.isInterface) {
            Proxy.getProxyClass(clazz.classLoader, clazz) as Class<T>
        } else {
            clazz
        }
}
