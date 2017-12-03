package io.mockk.impl

import io.mockk.*

internal class SignatureMatcherDetector(
        val callRounds: List<CallRound>,
        val childMocks: List<Ref>,
        val gateway: MockKGatewayImpl) {

    val nCalls = callRounds[0].calls.size

    fun detect(): List<MatchedCall> {
        val result = mutableListOf<MatchedCall>();

        checkAllSameNumberOfCalls()
        repeat(nCalls) { callN ->

            val detector = ChainedCallDetector(callRounds, childMocks, callN)

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
                    callRounds.map { it.calls.map { it.invocation }.joinToString(", ") }.joinToString("\n"))
        }
    }
}

