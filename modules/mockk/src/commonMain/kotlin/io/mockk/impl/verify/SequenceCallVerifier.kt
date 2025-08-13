package io.mockk.impl.verify

import io.mockk.MockKGateway
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeToString
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class SequenceCallVerifier(
    val stubRepo: StubRepository,
    val safeToString: SafeToString
) : MockKGateway.CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters
    ): VerificationResult {

        val allCalls = verificationSequence.allInvocations(stubRepo)

        if (allCalls.size != verificationSequence.size) {
            return VerificationResult.Failure(safeToString.exec {
                "number of calls happened not matching exact number of verification sequence" + reportCalls(
                    verificationSequence,
                    allCalls
                )
            })
        }

        for ((i, call) in allCalls.withIndex()) {
            val matcher = verificationSequence[i].matcher
            if (!matcher.match(call)) {
                return VerificationResult.Failure(safeToString.exec {
                    "calls are not exactly matching verification sequence" + reportCalls(verificationSequence,
                        allCalls)
                })
            }
            captureBlocks.add { matcher.captureAnswer(call) }
        }


        return VerificationResult.OK(allCalls)
    }

    override fun captureArguments() = captureBlocks.forEach { it() }
}
