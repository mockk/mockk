package io.mockk.impl.recording

import io.mockk.MatchedCall
import io.mockk.MockKException
import io.mockk.Ref

class  SignatureMatcherDetector(
        val callRounds: List<CallRound>,
        val childMocks: List<Ref>,
        val chainedCallDetectorFactory: ChainedCallDetectorFactory) {

    val nCalls = callRounds[0].calls.size

    fun detect(): List<MatchedCall> {
        val result = mutableListOf<MatchedCall>();

        checkAllSameNumberOfCalls()
        repeat(nCalls) { callN ->
            val detector = chainedCallDetectorFactory(callRounds, childMocks, callN)

            detector.gatherMatchers()
            detector.detectArgMatchers()
            detector.processCompositeMatchers()

            result.add(detector.buildChainedCall())
        }
        return result
    }

    private fun checkAllSameNumberOfCalls() {
        if (callRounds.any { it.calls.size != nCalls }) {
            throw MockKException("every/verify {} block were run several times. Recorded calls count differ between runs\n" +
                    callRounds.withIndex().map { "Round ${it.index + 1}: " + it.value.calls.map { it.invocation }.joinToString(", ") }.joinToString("\n"))
        }
    }
}

