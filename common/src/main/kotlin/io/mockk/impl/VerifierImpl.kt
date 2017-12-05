package io.mockk.impl

import io.mockk.CapturingSlot
import io.mockk.MockKGateway.*
import io.mockk.MockKVerificationScope
import io.mockk.common.StubRepository

class VerifierImpl(callRecorder: () -> CallRecorder,
                   val stubRepo: StubRepository,
                   autoHinterFactory: () -> AutoHinter) : CommonRecorder(callRecorder, autoHinterFactory), Verifier {

    override fun verify(params: VerificationParameters,
                        mockBlock: (MockKVerificationScope.() -> Unit)?,
                        coMockBlock: (suspend MockKVerificationScope.() -> Unit)?) {

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