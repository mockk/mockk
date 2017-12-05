package io.mockk.impl

import io.mockk.CapturingSlot
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.Stubber
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope

class StubberImpl(callRecorder: () -> CallRecorder,
                  autoHinterFactory: () -> AutoHinter) : CommonRecorder(callRecorder, autoHinterFactory), Stubber {

    override fun <T> every(mockBlock: (MockKMatcherScope.() -> T)?,
                           coMockBlock: (suspend MockKMatcherScope.() -> T)?): MockKStubScope<T> {

        callRecorder().startStubbing()

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        record(scope, mockBlock, coMockBlock)

        return MockKStubScope(callRecorder(), lambda)
    }
}
