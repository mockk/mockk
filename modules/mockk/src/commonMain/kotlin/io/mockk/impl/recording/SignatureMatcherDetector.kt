package io.mockk.impl.recording

import io.mockk.CompositeMatcher
import io.mockk.Matcher
import io.mockk.MockKException
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString

class SignatureMatcherDetector(
    safeToString: SafeToString,
    val chainedCallDetectorFactory: ChainedCallDetectorFactory
) {
    val calls = mutableListOf<RecordedCall>()
    val log = safeToString(Logger<SignatureMatcherDetector>())

    fun detect(callRounds: List<CallRound>) {
        calls.clear()

        val nCalls = callRounds[0].calls.size

        fun checkAllSameNumberOfCalls() {
            if (callRounds.any { it.calls.size != nCalls }) {
                throw MockKException("every/verify {} block were run several times. Recorded calls count differ between runs\n" +
                        callRounds.withIndex().joinToString("\n") { (index, value) ->
                            val calls = value.calls.joinToString(", ") { it.invocationStr }
                            "Round ${index + 1}: $calls"
                        }
                )
            }
        }

        val nMatchers = callRounds[0].matchers.size
        fun checkAllSameNumberOfMatchers() {
            if (callRounds.any { it.matchers.size != nMatchers }) {
                throw MockKException("every/verify {} block were run several times. Recorded matchers count differ between runs\n" +
                        callRounds.withIndex().joinToString("\n") { (index, value) ->
                            val matchers = value.matchers.joinToString(", ") { it.toString() }
                            "Round ${index + 1}: $matchers"
                        }
                )
            }
        }

        val matcherMap = hashMapOf<List<Any>, Matcher<*>>()
        val allCompositeMatchers = mutableListOf<List<CompositeMatcher<*>>>()

        fun gatherMatchers() {
            repeat(nMatchers) { nMatcher ->
                val matcher = callRounds.last().matchers[nMatcher].matcher
                val signature = callRounds.map { it.matchers[nMatcher].signature }

                if (matcher is CompositeMatcher<*>) {
                    allCompositeMatchers.add(callRounds.map {
                        it.matchers[nMatcher].matcher as CompositeMatcher<*>
                    })
                }

                matcherMap[signature] = matcher
            }

            log.trace { "Matcher map: $matcherMap" }
        }

        @Suppress("UNCHECKED_CAST")
        fun processCompositeMatchers() {
            for (compositeMatchers in allCompositeMatchers) {
                val matcher = compositeMatchers.last()

                matcher.subMatchers = matcher.operandValues.withIndex().map { (nOp, _) ->
                    val signature = compositeMatchers.map {
                        InternalPlatform.packRef(it.operandValues[nOp])
                    }.toList()

                    log.trace { "Signature for $nOp operand of $matcher composite matcher: $signature" }

                    matcherMap.remove(signature)
                        ?: ChainedCallDetector.eqOrNullMatcher(matcher.operandValues[nOp])
                } as List<Matcher<Any?>>?
            }
        }

        checkAllSameNumberOfCalls()
        checkAllSameNumberOfMatchers()

        gatherMatchers()

        repeat(nCalls) { callN ->
            val detector = chainedCallDetectorFactory()
            detector.detect(callRounds, callN, matcherMap)
            calls.add(detector.call)
        }

        processCompositeMatchers()
        if (matcherMap.isNotEmpty()) {
            throw MockKException("Failed matching mocking signature for\n${callRounds[0].calls.joinToString("\n")}\nleft matchers: ${matcherMap.values}")
        }
    }
}
