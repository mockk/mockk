package io.mockk.impl

import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.Stubber
import io.mockk.slot


internal class StubberImpl(gw: MockKGatewayImpl) : CommonRecorder(gw), Stubber {
    override fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                           coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T> {
        gw.callRecorder.startStubbing()
        val lambda = slot<Function<*>>()
        val scope = MockKMatcherScope(gw, lambda)
        try {
            record(scope, mockBlock, coMockBlock)
        } catch (ex: NoClassDefFoundError) {
            gw.callRecorder.cancel()
            throw prettifyCoroutinesException(ex)
        } catch (ex: Exception) {
            gw.callRecorder.cancel()
            throw ex
        }
        return MockKStubScope(gw, lambda)
    }

}
