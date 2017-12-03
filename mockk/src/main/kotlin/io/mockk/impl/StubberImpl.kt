package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.MockKGateway.*
import io.mockk.slot


internal class StubberImpl(gw: MockKGatewayImpl) : CommonRecorder(gw), Stubber {
    override fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                           coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T> {
        val callRecorder = gateway.callRecorder
        callRecorder.startStubbing()
        val lambda = slot<Function<*>>()
        val scope = MockKMatcherScope(gateway, lambda)
        try {
            record(scope, mockBlock, coMockBlock)
        } catch (ex: NoClassDefFoundError) {
            callRecorder.cancel()
            throw prettifyCoroutinesException(ex)
        } catch (ex: Throwable) {
            callRecorder.cancel()
            throw ex
        }
        checkMissingCalls()
        return MockKStubScope(gateway, lambda)
    }

    fun checkMissingCalls() {
        if (gateway.callRecorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside every { ... } block.")
        }
    }

}
