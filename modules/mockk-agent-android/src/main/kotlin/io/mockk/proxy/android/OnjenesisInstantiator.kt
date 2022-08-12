@file:Suppress("UNCHECKED_CAST")

package io.mockk.proxy.android

import com.android.dx.stock.ProxyBuilder
import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.MockKInstantiatior
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Modifier
import java.util.*

internal class OnjenesisInstantiator(val log: MockKAgentLogger) : MockKInstantiatior {
    private val objenesis = ObjenesisStd(false)
    private val instantiators = Collections.synchronizedMap(WeakHashMap<Class<*>, ObjectInstantiator<*>>())

    override fun <T> instance(cls: Class<T>): T {
        val clazz = proxyInterface(cls)

        log.trace("Creating new empty instance of $clazz")

        val inst = synchronized(instantiators) {
            instantiators.getOrPut(clazz) {
                objenesis.getInstantiatorOf(clazz)
            }
        }

        val newInstance = inst.newInstance()
        return clazz.cast(newInstance) as T
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> proxyInterface(clazz: Class<T>) =
        if (Modifier.isAbstract(clazz.modifiers)) {
            if (clazz.isInterface) {
                ProxyBuilder.forClass(Any::class.java)
                    .parentClassLoader(clazz.classLoader)
                    .implementing(clazz)
                    .buildProxyClass()
            } else {
                ProxyBuilder.forClass(clazz)
                    .parentClassLoader(clazz.classLoader)
                    .buildProxyClass()
            }
        } else {
            clazz
        }
}
