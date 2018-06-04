package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.MockKGateway.ObjectMockFactory
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.SpyKStub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKProxyMaker

class JvmObjectMockFactory(
    val proxyMaker: MockKProxyMaker,
    val stubRepository: StubRepository,
    val gatewayAccess: StubGatewayAccess
) : ObjectMockFactory {
    val refCntMap = RefCounterMap<Any>()

    override fun objectMockk(obj: Any, recordPrivateCalls: Boolean): () -> Unit {
        if (refCntMap.incrementRefCnt(obj)) {
            val cls = obj::class

            log.debug { "Creating object mockk for ${cls.toStr()}" }

            val stub = SpyKStub(cls, "object " + cls.simpleName, gatewayAccess, recordPrivateCalls)

            log.trace {
                "Building object proxy for ${cls.toStr()} hashcode=${InternalPlatform.hkd(
                    cls
                )}"
            }
            val cancellable = try {
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

            stub.disposeRoutine = cancellable::cancel

            stubRepository.add(obj, stub)
        }

        return {
            if (refCntMap.decrementRefCnt(obj)) {
                val stub = stubRepository.remove(obj)
                stub?.let {
                    log.debug {
                        "Disposing object mockk for ${obj::class.toStr()} hashcode=${InternalPlatform.hkd(
                            obj
                        )}"
                    }
                    it.dispose()
                }
            }
        }
    }

    override fun clear(
        obj: Any,
        answers: Boolean,
        recordedCalls: Boolean,
        childMocks: Boolean
    ) {
        stubRepository[obj]?.clear(answers, recordedCalls, childMocks)
    }

    companion object {
        val log = Logger<JvmStaticMockFactory>()
    }
}