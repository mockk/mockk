package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway.ObjectMockFactory
import io.mockk.agent.MockKAgentException
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.proxy.MockKProxyMaker

class JvmObjectMockFactory(val proxyMaker: MockKProxyMaker,
                           val stubRepository: StubRepository,
                           val gatewayAccess: StubGatewayAccess
) : ObjectMockFactory {

    override fun objectMockk(obj: Any, recordPrivateCalls: Boolean) {
        val cls = obj::class

        JvmStaticMockFactory.log.debug { "Creating object mockk for ${cls.toStr()}" }

        val stub = SpyKStub(cls, "object " + cls.simpleName, gatewayAccess, recordPrivateCalls)

        JvmStaticMockFactory.log.trace { "Building object proxy for ${cls.toStr()} hashcode=${InternalPlatform.hkd(cls)}" }
        try {
            proxyMaker.proxy(
                cls.java,
                emptyArray(),
                JvmMockFactoryHelper.mockHandler(stub),
                false,
                obj
            )
        } catch (ex: MockKAgentException) {
            throw MockKException("Failed to build object proxy", ex)
        }

        stub.hashCodeStr = InternalPlatform.hkd(cls.java)

        stubRepository.add(obj, stub)
    }

    override fun objectUnMockk(obj: Any) {
        proxyMaker.unproxy(obj)

        stubRepository.remove(obj)
    }
}