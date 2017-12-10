package io.mockk.impl.verify

import io.mockk.MatchedCall
import io.mockk.MockKGateway
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class SequenceCallVerifier(val stubRepo: StubRepository) : MockKGateway.CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(calls: List<MatchedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val allCalls = calls.allInvocations(stubRepo)

        if (allCalls.size != calls.size) {
            return MockKGateway.VerificationResult(false, "number of calls happened not matching exact number of verification sequence" + reportCalls(calls, allCalls))
        }

        for ((i, call) in allCalls.withIndex()) {
            if (!calls[i].matcher.match(call)) {
                return MockKGateway.VerificationResult(false, "calls are not exactly matching verification sequence" + reportCalls(calls, allCalls))
            }
            captureBlocks.add { calls[i].matcher.captureAnswer(call) }
        }


        return MockKGateway.VerificationResult(true)
    }

    override fun captureArguments() {
        captureBlocks.forEach { it() }
    }
}