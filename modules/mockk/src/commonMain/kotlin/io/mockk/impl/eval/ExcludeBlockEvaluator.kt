package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.Excluder
import io.mockk.MockKGateway.ExclusionParameters
import io.mockk.MockKMatcherScope
import io.mockk.impl.recording.AutoHinter
import io.mockk.impl.stub.StubRepository

class ExcludeBlockEvaluator(
    callRecorder: () -> CallRecorder,
    val stubRepo: StubRepository,
    autoHinterFactory: () -> AutoHinter,
) : RecordedBlockEvaluator(callRecorder, autoHinterFactory),
    Excluder {
    override fun exclude(
        params: ExclusionParameters,
        mockBlock: (MockKMatcherScope.() -> Unit)?,
        coMockBlock: (suspend MockKMatcherScope.() -> Unit)?,
    ) {
        if (coMockBlock != null) {
            initializeCoroutines()
        }

        callRecorder().startExclusion(params)

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKMatcherScope(callRecorder(), lambda)

        // On failure record() cancels the session itself and restores a suspended parent
        // session; resetting here as well would drop that parent a second time (issue #1398).
        record(scope, mockBlock, coMockBlock)
        callRecorder().reset()
    }
}
