package io.mockk.impl

import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.Stubber
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.slot

internal class StubberImpl(callRecorder: () -> CallRecorder) : CommonRecorder(callRecorder), Stubber {

    override fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                           coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T> {

        callRecorder().startStubbing()

        val lambda = slot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        record(scope, mockBlock, coMockBlock)

        return MockKStubScope(callRecorder(), lambda)
    }
}
