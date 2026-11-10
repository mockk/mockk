package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.Stubber
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.impl.recording.AutoHinter
import io.mockk.impl.stub.AnswerAnsweringOpportunity

class EveryBlockEvaluator(
    callRecorder: () -> CallRecorder,
    autoHinterFactory: () -> AutoHinter,
) : RecordedBlockEvaluator(callRecorder, autoHinterFactory),
    Stubber {
    @Suppress("UNCHECKED_CAST")
    override fun <T> every(
        mockBlock: (MockKMatcherScope.() -> T)?,
        coMockBlock: (suspend MockKMatcherScope.() -> T)?,
        lenient: Boolean,
    ): MockKStubScope<T, T> {
        if (coMockBlock != null) {
            initializeCoroutines()
        }

        callRecorder().startStubbing()

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        record(scope, mockBlock, coMockBlock)

        val opportunity = callRecorder().answerOpportunity()

        if (lenient) {
            (opportunity as? AnswerAnsweringOpportunity<*>)?.markLenient()
        }

        return MockKStubScope(opportunity as MockKGateway.AnswerOpportunity<T>, callRecorder(), lambda)
    }
}
