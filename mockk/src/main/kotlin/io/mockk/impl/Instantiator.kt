package io.mockk.impl

import io.mockk.InternalPlatform.toStr
import io.mockk.MockKException
import io.mockk.impl.MockKGatewayImpl.Stub
import io.mockk.agent.MockKAgentException
import io.mockk.impl.MockKGatewayImpl.Instantiator
import io.mockk.proxy.MockKProxyMaker
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Callable
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

internal class InstantiatorImpl(private val gateway: MockKGatewayImpl) : Instantiator {

    override fun <T : Any> instantiate(cls: KClass<T>): T {
        log.trace { "Building empty instance ${cls.toStr()}" }

        for (factory in gateway.factoryRegistryIntrnl.instanceFactories) {
            val instance = factory.instantiate(cls)
            if (instance != null) {
                log.trace { "Instance factory returned instance $instance" }
                return cls.cast(instance)
            }
        }

        return MockKProxyMaker.INSTANCE.instance(cls.java)
    }

    override fun isPassedByValue(cls: KClass<*>): Boolean {
        return when (cls) {
            java.lang.Boolean::class -> true
            java.lang.Byte::class -> true
            java.lang.Short::class -> true
            java.lang.Character::class -> true
            java.lang.Integer::class -> true
            java.lang.Long::class -> true
            java.lang.Float::class -> true
            java.lang.Double::class -> true
            java.lang.String::class -> true
            else -> false
        }
    }


    companion object {
        val log = logger<InstantiatorImpl>()
    }
}
