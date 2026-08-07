package io.mockk.impl.stub

import io.mockk.Invocation
import kotlin.reflect.KClass

class SpyKStub<T : Any>(
    cls: KClass<out T>,
    name: String,
    gatewayAccess: StubGatewayAccess,
    recordPrivateCalls: Boolean,
    mockType: MockType,
) : MockKStub(cls, name, false, false, gatewayAccess, recordPrivateCalls, mockType) {
    override fun defaultAnswer(invocation: Invocation): Any? =
        if (isSuppressed(invocation)) {
            relaxedValue(invocation)
        } else {
            invocation.originalCall()
        }

    private fun relaxedValue(invocation: Invocation): Any? {
        if (invocation.method.returnsUnit) return Unit
        return gatewayAccess.anyValueGenerator().anyValue(
            invocation.method.returnType,
            invocation.method.returnTypeNullable,
        ) {
            childMockK(invocation.allEqMatcher(), invocation.method.returnType)
        }
    }
}
