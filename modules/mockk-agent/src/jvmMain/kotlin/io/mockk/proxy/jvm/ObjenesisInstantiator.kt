package io.mockk.proxy.jvm

import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.MockKInstantiatior
import io.mockk.proxy.jvm.transformation.CacheKey
import net.bytebuddy.ByteBuddy
import net.bytebuddy.TypeCache
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Modifier
import java.util.Collections.synchronizedMap
import java.util.WeakHashMap

class ObjenesisInstantiator(
    private val log: MockKAgentLogger,
    private val byteBuddy: ByteBuddy,
) : MockKInstantiatior {
    private val objenesis = ObjenesisStd(false)

    private val typeCache = TypeCache<CacheKey>(TypeCache.Sort.WEAK)

    private val instantiators = synchronizedMap(WeakHashMap<Class<*>, ObjectInstantiator<*>>())

    override fun <T> instance(cls: Class<T>): T {
        if (cls == Any::class.java) {
            @Suppress("UNCHECKED_CAST")
            return Any() as T
        } else if (!Modifier.isFinal(cls.modifiers)) {
            try {
                val instance = instantiateViaProxy(cls)
                if (instance != null) {
                    return instance
                }
            } catch (ex: MockKAgentException) {
                throw ex
            } catch (ex: Exception) {
                log.trace(
                    ex,
                    "Failed to instantiate via proxy " + cls + ". " +
                        "Doing objenesis instantiation",
                )
            }
        }

        return instanceViaObjenesis(cls)
    }

    private fun <T> instantiateViaProxy(cls: Class<T>): T? {
        val proxyCls =
            if (!Modifier.isAbstract(cls.modifiers)) {
                log.trace("Skipping instantiation subsclassing $cls because class is not abstract.")
                cls
            } else {
                log.trace("Instantiating $cls via subclass proxy")

                val classLoader = cls.classLoader
                typeCache.findOrInsert(
                    classLoader,
                    CacheKey(cls, setOf()),
                    {
                        byteBuddy
                            .subclass(cls)
                            .annotateType(*cls.annotations)
                            .make()
                            .load(classLoader)
                            .loaded
                    },
                    classLoader ?: bootstrapMonitor,
                )
            }

        return cls.cast(instanceViaObjenesis(proxyCls))
    }

    private fun <T> instanceViaObjenesis(clazz: Class<T>): T {
        log.trace("Creating new empty instance of $clazz")

        try {
            return clazz.cast(getOrCreateInstantiator(clazz).newInstance())
        } catch (ex: Exception) {
            throw failedInstantiation(clazz, ex)
        } catch (ex: LinkageError) {
            throw failedInstantiation(clazz, ex)
        }
    }

    private fun <T> getOrCreateInstantiator(clazz: Class<T>): ObjectInstantiator<*> =
        instantiators[clazz] ?: let {
            objenesis.getInstantiatorOf(clazz).also {
                instantiators[clazz] = it
            }
        }

    /**
     * Allocating the instance initializes the class, so a mock cannot be created for a class
     * whose static initializer fails: on JDK 24+ the standard objenesis strategy fails while the
     * serialization constructor accessor is created (`ReflectionFactory.newConstructorForSerialization`
     * initializes the class), on older JDKs the failure surfaces from `newInstance` itself. The
     * initially reported error hides the real cause several levels deep — or, once the revert
     * retransformation of the class left in an erroneous state also fails, not at all (#1538) —
     * so report the initialization failure directly.
     */
    private fun <T> failedInstantiation(
        clazz: Class<T>,
        error: Throwable,
    ): MockKAgentException {
        val cause = linkageCause(error) ?: error
        return MockKAgentException(
            "Failed to create an instance of $clazz for mocking: the class could not be " +
                "initialized. Creating a mock instance runs the class's static initializer " +
                "(<clinit>), which failed, so mocks of this class cannot be created.",
            cause,
        )
    }

    private fun linkageCause(error: Throwable): Throwable? {
        var current: Throwable? = error
        while (current != null && current !== current.cause) {
            if (current is LinkageError) return current
            current = current.cause
        }
        return null
    }

    companion object {
        private val bootstrapMonitor = Any()
    }
}
