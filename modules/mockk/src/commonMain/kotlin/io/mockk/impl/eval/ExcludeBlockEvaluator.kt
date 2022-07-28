package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKGateway.*
import io.mockk.MockKMatcherScope
import io.mockk.impl.recording.AutoHinter
import io.mockk.impl.stub.StubRepository

class ExcludeBlockEvaluator(
    callRecorder: () -> CallRecorder,
    val stubRepo: StubRepository,
    autoHinterFactory: () -> AutoHinter
) : RecordedBlockEvaluator(callRecorder, autoHinterFactory), Excluder {

    override fun exclude(
        params: ExclusionParameters,
        mockBlock: (MockKMatcherScope.() -> Unit)?,
        coMockBlock: (suspend MockKMatcherScope.() -> Unit)?
    ) {
        if (coMockBlock != null) {
            initializeCoroutines()
        }

        callRecorder().startExclusion(params)

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        try {
            record(scope, mockBlock, coMockBlock)
        } finally {
            callRecorder().reset()
        }
    }
}