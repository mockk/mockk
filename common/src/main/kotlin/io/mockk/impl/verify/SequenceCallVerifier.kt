package io.mockk.impl.verify

import io.mockk.MockKGateway
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeLog
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class SequenceCallVerifier(
    val stubRepo: StubRepository,
    val safeLog: SafeLog
) : MockKGateway.CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(verificationSequence: List<RecordedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val allCalls = verificationSequence.allInvocations(stubRepo)

        if (allCalls.size != verificationSequence.size) {
            return MockKGateway.VerificationResult(false, safeLog.exec {
                "number of calls happened not matching exact number of verification sequence" + reportCalls(
                    verificationSequence,
                    allCalls
                )
            })
        }

        for ((i, call) in allCalls.withIndex()) {
            val matcher = verificationSequence[i].matcher
            if (!matcher.match(call)) {
                return MockKGateway.VerificationResult(false, safeLog.exec {
                    "calls are not exactly matching verification sequence" + reportCalls(verificationSequence, allCalls)
                })
            }
            captureBlocks.add { matcher.captureAnswer(call) }
        }


        return MockKGateway.VerificationResult(true)
    }

    override fun captureArguments() {
        captureBlocks.forEach { it() }
    }
}