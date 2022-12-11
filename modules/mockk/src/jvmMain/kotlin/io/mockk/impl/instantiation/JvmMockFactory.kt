package io.mockk.impl.instantiation

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKException
import io.mockk.impl.stub.MockKStub
import io.mockk.impl.stub.Stub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKProxyMaker
import kotlin.reflect.KClass

class JvmMockFactory(
    val proxyMaker: MockKProxyMaker,
    instantiator: JvmInstantiator,
    stubRepository: StubRepository,
    gatewayAccess: StubGatewayAccess
) :
    AbstractMockFactory(
        stubRepository,
        instantiator,
        gatewayAccess
    ) {

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> newProxy(
        cls: KClass<out T>,
        moreInterfaces: Array<out KClass<*>>,
        stub: Stub,
        useDefaultConstructor: Boolean,
        instantiate: Boolean
    ): T {
        return try {
            val proxyResult = proxyMaker.proxy(
                cls.java,
                moreInterfaces.map { it.java }.toTypedArray(),
                JvmMockFactoryHelper.mockHandler(stub),
                useDefaultConstructor,
                null
            )

            (stub as? MockKStub)?.disposeRoutine = proxyResult::cancel

            proxyResult.get()
        } catch (ex: MockKAgentException) {
            when {
                instantiate -> {
                    log.trace(ex) {
                        "Failed to build proxy for ${cls.toStr()}. " +
                                "Trying just instantiate it. " +
                                "This can help if it's last call in the chain"
                    }

                    gatewayAccess.anyValueGenerator().anyValue(cls, isNullable = false) {
                        instantiator.instantiate(cls)
                    } as T
                }

                useDefaultConstructor ->
                    throw MockKException("Can't instantiate proxy via " +
                        "default constructor for $cls", ex)

                else ->
                    throw MockKException("Can't instantiate proxy for $cls", ex)
            }
        }
    }
}

