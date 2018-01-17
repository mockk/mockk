package io.mockk.impl.verify

import io.mockk.MockKGateway
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeLog
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class OrderedCallVerifier(
    val stubRepo: StubRepository,
    val safeLog: SafeLog
) : MockKGateway.CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(verificationSequence: List<RecordedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val allCalls = verificationSequence.allInvocations(stubRepo)

        if (verificationSequence.size > allCalls.size) {
            return MockKGateway.VerificationResult(false, safeLog.exec {
                "less calls happened then demanded by order verification sequence. " +
                        reportCalls(verificationSequence, allCalls)
            })
        }

        // LCS algorithm
        val nEdits = Array(allCalls.size, { Array(verificationSequence.size, { 0 }) })
        val path = Array(allCalls.size, { Array(verificationSequence.size, { '?' }) })

        fun maxOf(a: Pair<Int, Char>, b: Pair<Int, Char>) =
            if (a.first > b.first) a else b

        for ((callIdx, call) in allCalls.withIndex()) {
            for ((matcherIdx, matcher) in verificationSequence.map { it.matcher }.withIndex()) {

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
                            Pair(nEdits[callIdx][matcherIdx - 1], '<')
                    )
                }

                nEdits[callIdx][matcherIdx] = result.first
                path[callIdx][matcherIdx] = result.second
            }
        }

        // match only if all matchers present
        if (nEdits[allCalls.size - 1][verificationSequence.size - 1] == verificationSequence.size) {

            tailrec fun backTrackCalls(callIdx: Int, matcherIdx: Int) {
                if (callIdx < 0 || matcherIdx < 0) return

                when (path[callIdx][matcherIdx]) {
                    '=' -> {
                        val matcher = verificationSequence[matcherIdx].matcher
                        val invocation = allCalls[callIdx]
                        captureBlocks.add { matcher.captureAnswer(invocation) }
                        backTrackCalls(callIdx - 1, matcherIdx - 1)
                    }
                    '^' -> {
                        backTrackCalls(callIdx - 1, matcherIdx)
                    }
                    '<' -> {
                        backTrackCalls(callIdx, matcherIdx - 1)
                    }
                }
            }

            backTrackCalls(allCalls.size - 1, verificationSequence.size - 1)

            return MockKGateway.VerificationResult(true)
        } else {
            return MockKGateway.VerificationResult(false, safeLog.exec {
                "calls are not in verification order" + reportCalls(verificationSequence, allCalls)
            })
        }
    }

    override fun captureArguments() {
        captureBlocks.forEach { it() }
    }

}