package io.mockk.impl.recording

import io.mockk.MockKException
import io.mockk.RecordedCall

class SignatureMatcherDetector(val chainedCallDetectorFactory: ChainedCallDetectorFactory) {
    val calls = mutableListOf<RecordedCall>()

    fun detect(callRounds: List<CallRound>) {
        calls.clear()

        val nCalls = callRounds[0].calls.size

        fun checkAllSameNumberOfCalls() {
            if (callRounds.any { it.calls.size != nCalls }) {
                throw MockKException("every/verify {} block were run several times. Recorded calls count differ between runs\n" +
                        callRounds.withIndex().map {
                            "Round ${it.index + 1}: " + it.value.calls.map { it.invocationStr }.joinToString(
                                ", "
                            )
                        }.joinToString("\n")
                )
            }
        }

        checkAllSameNumberOfCalls()
        repeat(nCalls) { callN ->
            val detector = chainedCallDetectorFactory()
            detector.detect(callRounds, callN)
            calls.add(detector.call)
        }
    }
}

