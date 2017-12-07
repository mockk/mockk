package io.mockk.jvm

import io.mockk.InternalPlatform.hkd
import io.mockk.InternalPlatform.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway.StaticMockFactory
import io.mockk.agent.MockKAgentException
import io.mockk.common.StubRepository
import io.mockk.impl.Logger
import io.mockk.impl.MockKStub
import io.mockk.proxy.MockKProxyMaker
import kotlin.reflect.KClass

class JvmStaticMockFactory(val proxyMaker: MockKProxyMaker,
                           val stubRepository: StubRepository) : StaticMockFactory {
    override fun staticMockk(cls: KClass<*>) {
        log.debug { "Creating static mockk for ${cls.toStr()}" }

        val stub = MockKStub(cls, "static " + cls.simpleName)

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
    }


    companion object {
        val log = Logger<JvmStaticMockFactory>()
    }
}

