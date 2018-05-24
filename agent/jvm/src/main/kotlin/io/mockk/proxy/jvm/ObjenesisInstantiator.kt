package io.mockk.proxy.jvm

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.MockKInstantiatior
import io.mockk.proxy.jvm.transformation.CacheKey
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Modifier
import java.util.*

class ObjenesisInstantiator(
    private val log: MockKAgentLogger,
    private val byteBuddy: ByteBuddy
) : MockKInstantiatior {
    private val objenesis = ObjenesisStd(true)

    private val typeCache = TypeCache<CacheKey>(TypeCache.Sort.WEAK)

    private val instantiators = Collections.synchronizedMap(WeakHashMap<Class<*>, ObjectInstantiator<*>>())

    override fun <T> instance(cls: Class<T>): T  {
        if (!Modifier.isFinal(cls.modifiers)) {
            try {
                val instance = instantiateViaProxy(cls)
                if (instance != null) {
                    return instance
                }
            } catch (ex: Exception) {
                log.trace(
                    ex, "Failed to instantiate via proxy " + cls + ". " +
                            "Doing objenesis instantiation"
                )
            }

        }

        return instanceViaObjenesis(cls)
    }

    private fun <T> instantiateViaProxy(cls: Class<T>): T? {
        log.trace("Instantiating $cls via subclass proxy")

        val classLoader = cls.classLoader
        val monitor = classLoader ?: bootstrapMonitor
        val proxyCls = typeCache.findOrInsert(
            classLoader,
            CacheKey(cls, setOf()),
            {
                byteBuddy.subclass(cls)
                    .make()
                    .load(classLoader)
                    .loaded
            }, monitor
        )

        return cls.cast(instanceViaObjenesis(proxyCls))
    }


    private fun <T> instanceViaObjenesis(clazz: Class<T>): T {
        log.trace("Creating new empty instance of $clazz")

        return clazz.cast(
            getOrCreateInstantiator(clazz)
                .newInstance()
        )
    }

    private fun <T> getOrCreateInstantiator(clazz: Class<T>) =
        instantiators[clazz] ?: let {
            objenesis.getInstantiatorOf(clazz).also {
                instantiators[clazz] = it
            }
        }

    companion object {
        private val bootstrapMonitor = Any()
    }
}
