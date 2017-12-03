package io.mockk.impl

import io.mockk.MatchedCall
import io.mockk.MockKGateway
import io.mockk.impl.VerificationHelpers.allInvocations
import io.mockk.impl.VerificationHelpers.reportCalls

internal class SequenceCallVerifierImpl(val stubRepo: StubRepository) : MockKGateway.CallVerifier {
    override fun verify(calls: List<MatchedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val allCalls = calls.allInvocations(stubRepo)

        if (allCalls.size != calls.size) {
            return MockKGateway.VerificationResult(false, "number of calls happened not matching exact number of verification sequence" + reportCalls(calls, allCalls))
        }

        for ((i, call) in allCalls.withIndex()) {
            if (!calls[i].matcher.match(call)) {
                return MockKGateway.VerificationResult(false, "calls are not exactly matching verification sequence" + reportCalls(calls, allCalls))
            }
        }

        return MockKGateway.VerificationResult(true)
    }
}