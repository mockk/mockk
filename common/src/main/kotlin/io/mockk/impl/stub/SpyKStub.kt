package io.mockk.impl.stub

import io.mockk.Invocation
import kotlin.reflect.KClass

class SpyKStub<T : Any>(
    cls: KClass<out T>,
    name: String,
    gatewayAccess: StubGatewayAccess,
    recordPrivateCalls: Boolean
) : MockKStub(cls, name, false, gatewayAccess, recordPrivateCalls) {

    override fun defaultAnswer(invocation: Invocation): Any? {
        return invocation.originalCall()
    }
}