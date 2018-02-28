package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway.StaticMockFactory
import io.mockk.agent.MockKAgentException
import io.mockk.impl.InternalPlatform.hkd
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.proxy.MockKProxyMaker
import kotlin.reflect.KClass

class JvmStaticMockFactory(
    val proxyMaker: MockKProxyMaker,
    val stubRepository: StubRepository,
    val gatewayAccess: StubGatewayAccess
) : StaticMockFactory {
    override fun staticMockk(cls: KClass<*>) {
        log.debug { "Creating static mockk for ${cls.toStr()}" }

        val stub = SpyKStub(cls, "static " + cls.simpleName, gatewayAccess, true)

        log.trace { "Building static proxy for ${cls.toStr()} hashcode=${hkd(cls)}" }
        try {
            proxyMaker.staticProxy(cls.java, JvmMockFactoryHelper.mockHandler(stub))
        } catch (ex: MockKAgentException) {
            throw MockKException("Failed to build static proxy", ex)
        }

        stub.hashCodeStr = hkd(cls.java)

        stubRepository.add(cls.java, stub)
    }

    override fun staticUnMockk(cls: KClass<*>) {
        proxyMaker.staticUnProxy(cls.java)

        stubRepository.remove(cls.java)
    }


    companion object {
        val log = Logger<JvmStaticMockFactory>()
    }
}

