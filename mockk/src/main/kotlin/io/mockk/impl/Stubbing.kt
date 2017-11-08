package io.mockk.impl

import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.MockKGateway.*
import io.mockk.slot


internal class StubberImpl(gw: MockKGatewayImpl) : CommonRecorder(gw), Stubber {
    override fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                           coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T> {
        gateway.callRecorder.startStubbing()
        val lambda = slot<Function<*>>()
        val scope = MockKMatcherScope(gateway, lambda)
        try {
            record(scope, mockBlock, coMockBlock)
        } catch (ex: NoClassDefFoundError) {
            gateway.callRecorder.cancel()
            throw prettifyCoroutinesException(ex)
        } catch (ex: Exception) {
            gateway.callRecorder.cancel()
            throw ex
        }
        return MockKStubScope(gateway, lambda)
    }

}
