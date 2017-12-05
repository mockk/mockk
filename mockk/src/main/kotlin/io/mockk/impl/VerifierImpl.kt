package io.mockk.impl

import io.mockk.MockKGateway.*
import io.mockk.MockKVerificationScope
import io.mockk.slot

internal class VerifierImpl(callRecorder: () -> CallRecorder,
                            val stubRepo: StubRepository) : CommonRecorder(callRecorder), Verifier {

    override fun verify(params: VerificationParameters,
                        mockBlock: (MockKVerificationScope.() -> Unit)?,
                        coMockBlock: (suspend MockKVerificationScope.() -> Unit)?) {

        callRecorder().startVerification(params)

        val lambda = slot<Function<*>>()
        val scope = MockKVerificationScope(callRecorder(), lambda)

        try {
            record(scope, mockBlock, coMockBlock)
        } finally {
            callRecorder().reset()
        }
    }
}