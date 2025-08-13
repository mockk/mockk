package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.StaticMockFactory
import io.mockk.impl.InternalPlatform.hkd
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.MockType
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

            val stub = SpyKStub(
                cls,
                "static " + cls.simpleName,
                gatewayAccess,
                true,
                MockType.STATIC
            )

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
                val stub = stubRepository[cls.java]
                stub?.let {
                    log.debug { "Disposing static mockk for $cls" }
                    it.dispose()
                }
            }
        }
    }


    override fun clear(
        type: KClass<*>,
        options: MockKGateway.ClearOptions
    ) {
        stubRepository[type.java]?.clear(options)
    }

    override fun clearAll(
        options: MockKGateway.ClearOptions,
        currentThreadOnly: Boolean
    ) {
        val currentThreadId = Thread.currentThread().id
        stubRepository.allStubs.forEach {
            if (currentThreadOnly && currentThreadId != it.threadId) {
                return@forEach
            }
            it.clear(options)
        }
    }

    companion object {
        val log = Logger<JvmStaticMockFactory>()
    }
}
