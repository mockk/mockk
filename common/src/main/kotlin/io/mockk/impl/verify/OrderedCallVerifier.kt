package io.mockk.impl.verify

import io.mockk.MatchedCall
import io.mockk.MockKGateway
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class OrderedCallVerifier(val stubRepo: StubRepository) : MockKGateway.CallVerifier {
    override fun verify(calls: List<MatchedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val allCalls = calls.allInvocations(stubRepo)

        if (calls.size > allCalls.size) {
            return MockKGateway.VerificationResult(false, "less calls happened then demanded by order verification sequence. " +
                    reportCalls(calls, allCalls))
        }

        // LCS algorithm
        var prev = Array(calls.size, { 0 })
        var curr = Array(calls.size, { 0 })
        for (call in allCalls) {
            for ((matcherIdx, matcher) in calls.map { it.matcher }.withIndex()) {
                curr[matcherIdx] = if (matcher.match(call)) {
                    if (matcherIdx == 0) 1 else prev[matcherIdx - 1] + 1
                } else {
                    maxOf(prev[matcherIdx], if (matcherIdx == 0) 0 else curr[matcherIdx - 1])
                }
            }
            val swap = curr
            curr = prev
            prev = swap
        }

        // match only if all matchers present
        if (prev.last() == calls.size) {
            return MockKGateway.VerificationResult(true)
        } else {
            return MockKGateway.VerificationResult(false, "calls are not in verification order" + reportCalls(calls, allCalls))
        }
    }

}