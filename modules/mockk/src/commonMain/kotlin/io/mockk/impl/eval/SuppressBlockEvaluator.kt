package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.Suppresser
import io.mockk.MockKMatcherScope
import io.mockk.impl.recording.AutoHinter
import io.mockk.impl.stub.StubRepository

class SuppressBlockEvaluator(
    callRecorder: () -> CallRecorder,
    val stubRepo: StubRepository,
    autoHinterFactory: () -> AutoHinter,
) : RecordedBlockEvaluator(callRecorder, autoHinterFactory),
    Suppresser {
    override fun suppress(
        mockBlock: (MockKMatcherScope.() -> Unit)?,
        coMockBlock: (suspend MockKMatcherScope.() -> Unit)?,
    ) {
        if (coMockBlock != null) {
            initializeCoroutines()
        }

        callRecorder().startSuppression()

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        try {
            record(scope, mockBlock, coMockBlock)
        } finally {
            callRecorder().reset()
        }
    }
}
