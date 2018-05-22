package io.mockk.proxy.android

import io.mockk.agent.MockKAgentLogger
import io.mockk.agent.MockKInstantiatior
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Proxy
import java.util.*

internal class OnjenesisInstantiator(val log: MockKAgentLogger) : MockKInstantiatior {
    private val objenesis = ObjenesisStd(true)
    private val instantiators = Collections.synchronizedMap(WeakHashMap<Class<*>, ObjectInstantiator<*>>())

    override fun <T> instance(clazz: Class<T>): T {
        val cls = proxyInterface(clazz)

        log.trace("Creating new empty instance of $cls")

        val inst = instantiators.getOrPut(cls, {
            objenesis.getInstantiatorOf(cls)
        })

        return cls.cast(inst.newInstance())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxyInterface(clazz: Class<T>) =
        if (clazz.isInterface) {
            Proxy.getProxyClass(clazz.classLoader, clazz) as Class<T>
        } else {
            clazz
        }
}
