package io.mockk.impl.instantiation

import io.mockk.MethodDescription
import io.mockk.impl.log.Logger
import io.mockk.impl.stub.Stub
import io.mockk.impl.stub.StubGatewayAccess
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

class NativeMockFactory(
    stubRepository: StubRepository,
    instantiator: NativeInstantiator,
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
        TODO("proxy")
    }

    companion object {
        val log = Logger<NativeMockFactory>()
    }
}
