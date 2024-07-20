package io.mockk.impl.verify

import io.mockk.MockKGateway
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeToString
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class OrderedCallVerifier(
    val stubRepo: StubRepository,
    val safeToString: SafeToString
) : MockKGateway.CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters
    ): VerificationResult {

        val allCalls = verificationSequence.allInvocations(stubRepo)

        if (verificationSequence.size > allCalls.size) {
            return VerificationResult.Failure(safeToString.exec {
                "fewer calls happened than demanded by order verification sequence. " +
                        reportCalls(verificationSequence, allCalls)
            })
        }

        val lcs = LCSMatchingAlgo(allCalls, verificationSequence, captureBlocks)

        return if (lcs.lcs()) {
            VerificationResult.OK(lcs.verifiedCalls)
        } else {
            VerificationResult.Failure(safeToString.exec {
                "calls are not in verification order" + reportCalls(verificationSequence, allCalls, lcs)
            })
        }

    }

    override fun captureArguments() = captureBlocks.forEach { it() }

}
