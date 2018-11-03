package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKGateway.*
import io.mockk.MockKVerificationScope
import io.mockk.impl.recording.AutoHinter
import io.mockk.impl.stub.StubRepository

class VerifyBlockEvaluator(
    callRecorder: () -> CallRecorder,
    val stubRepo: StubRepository,
    autoHinterFactory: () -> AutoHinter
) : RecordedBlockEvaluator(callRecorder, autoHinterFactory), Verifier {

    override fun verify(
        params: VerificationParameters,
        mockBlock: (MockKVerificationScope.() -> Unit)?,
        coMockBlock: (suspend MockKVerificationScope.() -> Unit)?
    ) {

        callRecorder().startVerification(params)

        val lambda = CapturingSlot<Function<*>>()
        val scope = MockKVerificationScope(callRecorder(), lambda)

        try {
            record(scope, mockBlock, coMockBlock)
        } finally {
            callRecorder().reset()
        }
    }
}