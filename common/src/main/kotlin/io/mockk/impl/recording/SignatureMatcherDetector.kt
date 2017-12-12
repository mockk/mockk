package io.mockk.impl.recording

import io.mockk.MatchedCall
import io.mockk.MockKException
import io.mockk.Ref

class SignatureMatcherDetector(val chainedCallDetectorFactory: ChainedCallDetectorFactory) {

    fun detect(callRounds: List<CallRound>, childMocks: List<Ref>): List<MatchedCall> {
        val nCalls = callRounds[0].calls.size
        val result = mutableListOf<MatchedCall>();

        fun checkAllSameNumberOfCalls() {
            if (callRounds.any { it.calls.size != nCalls }) {
                throw MockKException("every/verify {} block were run several times. Recorded calls count differ between runs\n" +
                        callRounds.withIndex().map { "Round ${it.index + 1}: " + it.value.calls.map { it.invocation }.joinToString(", ") }.joinToString("\n"))
            }
        }

        checkAllSameNumberOfCalls()
        repeat(nCalls) { callN ->
            val detector = chainedCallDetectorFactory()

            val calls = detector.detect(callRounds, childMocks, callN)

            result.add(calls)
        }
        return result
    }

}

