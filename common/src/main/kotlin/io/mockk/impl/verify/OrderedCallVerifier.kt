package io.mockk.impl.verify

import io.mockk.MatchedCall
import io.mockk.MockKGateway
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class OrderedCallVerifier(val stubRepo: StubRepository) : MockKGateway.CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(matchedCalls: List<MatchedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val allCalls = matchedCalls.allInvocations(stubRepo)

        if (matchedCalls.size > allCalls.size) {
            return MockKGateway.VerificationResult(false, "less calls happened then demanded by order verification sequence. " +
                    reportCalls(matchedCalls, allCalls))
        }

        // LCS algorithm
        val nEdits = Array(allCalls.size, { Array(matchedCalls.size, { 0 }) })
        val path = Array(allCalls.size, { Array(matchedCalls.size, { '?' }) })

        fun maxOf(a: Pair<Int, Char>, b: Pair<Int, Char>) =
                if (a.first > b.first) a else b

        for ((callIdx, call) in allCalls.withIndex()) {
            for ((matcherIdx, matcher) in matchedCalls.map { it.matcher }.withIndex()) {

                val result = if (matcher.match(call)) {
                    if (matcherIdx == 0 || callIdx == 0)
                        Pair(1, '=')
                    else
                        Pair(nEdits[callIdx - 1][matcherIdx - 1] + 1, '=')
                } else {
                    maxOf(
                            if (callIdx == 0)
                                Pair(0, '^')
                            else
                                Pair(nEdits[callIdx - 1][matcherIdx], '='),

                            if (matcherIdx == 0)
                                Pair(0, '<')
                            else
                                Pair(nEdits[callIdx][matcherIdx - 1], '<'))
                }

                nEdits[callIdx][matcherIdx] = result.first
                path[callIdx][matcherIdx] = result.second
            }
        }

        // match only if all matchers present
        if (nEdits[allCalls.size - 1][matchedCalls.size - 1] == matchedCalls.size) {

            tailrec fun backTrackMatchedCalls(callIdx: Int, matcherIdx: Int) {
                if (callIdx < 0 || matcherIdx < 0) return

                when (path[callIdx][matcherIdx]) {
                    '=' -> {
                        captureBlocks.add { matchedCalls[matcherIdx].matcher.captureAnswer(allCalls[callIdx]) }
                        backTrackMatchedCalls(callIdx - 1, matcherIdx - 1)
                    }
                    '^' -> {
                        backTrackMatchedCalls(callIdx - 1, matcherIdx)
                    }
                    '<' -> {
                        backTrackMatchedCalls(callIdx, matcherIdx - 1)
                    }
                }
            }

            backTrackMatchedCalls(allCalls.size - 1, matchedCalls.size - 1)

            return MockKGateway.VerificationResult(true)
        } else {
            return MockKGateway.VerificationResult(false, "calls are not in verification order" + reportCalls(matchedCalls, allCalls))
        }
    }

    override fun captureArguments() {
        captureBlocks.forEach { it() }
    }

}