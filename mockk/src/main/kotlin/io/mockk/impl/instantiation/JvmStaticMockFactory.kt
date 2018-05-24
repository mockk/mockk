package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway.StaticMockFactory
import io.mockk.impl.InternalPlatform.hkd
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKStaticProxyMaker
import kotlin.reflect.KClass

class JvmStaticMockFactory(
    val proxyMaker: MockKStaticProxyMaker,
    val stubRepository: StubRepository,
    val gatewayAccess: StubGatewayAccess
) : StaticMockFactory {

    val refCntMap = RefCounterMap<KClass<*>>()

    override fun staticMockk(cls: KClass<*>): () -> Unit {
        if (refCntMap.incrementRefCnt(cls)) {
            log.debug { "Creating static mockk for ${cls.toStr()}" }

            val stub = SpyKStub(cls, "static " + cls.simpleName, gatewayAccess, true)

            log.trace { "Building static proxy for ${cls.toStr()} hashcode=${hkd(cls)}" }
            val cancellation = try {
                proxyMaker.staticProxy(cls.java, JvmMockFactoryHelper.mockHandler(stub))
            } catch (ex: MockKAgentException) {
                throw MockKException("Failed to build static proxy", ex)
            }

            stub.hashCodeStr = hkd(cls.java)
            stub.disposeRoutine = cancellation::cancel

            stubRepository.add(cls.java, stub)
        }

        return {
            if (refCntMap.decrementRefCnt(cls)) {
                val stub = stubRepository[cls]
                stub?.let {
                    log.debug { "Disposing static mockk for $cls" }
                    it.dispose()
                }
            }
        }
    }


    override fun clear(type: KClass<*>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        stubRepository.get(type.java)?.clear(answers, recordedCalls, childMocks)
    }

    companion object {
        val log = Logger<JvmStaticMockFactory>()
    }
}

