package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.Stubber
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.impl.recording.AutoHinter

class EveryBlockEvaluator(
    callRecorder: () -> CallRecorder,
    autoHinterFactory: () -> AutoHinter
) : RecordedBlockEvaluator(callRecorder, autoHinterFactory), Stubber {

    @Suppress("UNCHECKED_CAST")
    override fun <T> every(
        mockBlock: (MockKMatcherScope.() -> T)?,
        coMockBlock: (suspend MockKMatcherScope.() -> T)?
    ): MockKStubScope<T, T> {
        if (coMockBlock != null) {
            initializeCoroutines()
        }

        callRecorder().startStubbing()

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        record(scope, mockBlock, coMockBlock)

        val opportunity = callRecorder().answerOpportunity() as MockKGateway.AnswerOpportunity<T>

        return MockKStubScope(opportunity, callRecorder(), lambda)
    }
}
