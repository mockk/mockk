package io.mockk.impl

import io.mockk.*
import io.mockk.InternalPlatform.toStr
import io.mockk.impl.CallRecorderImpl.Companion.packRef
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.full.isSubclassOf

internal class SignatureMatcherDetector {
    @Suppress("UNCHECKED_CAST")
    fun detect(callRounds: List<CallRound>, childMocks: List<Ref>, gateway: MockKGatewayImpl): List<MatchedCall> {
        val nCalls = callRounds[0].calls.size
        if (callRounds.any { it.calls.size != nCalls }) {
            throw MockKException("every/verify {} block were run several times. Recorded calls count differ between runs\n" +
                    callRounds.map { it.calls.map { it.invocation }.joinToString(", ") }.joinToString("\n"))
        }

        val calls = mutableListOf<MatchedCall>();

        repeat(nCalls) { callN ->

            val callInAllRounds = callRounds.map { it.calls[callN] }
            val matcherMap = hashMapOf<List<Any>, Matcher<*>>()
            val compositeMatchers = mutableListOf<List<CompositeMatcher<*>>>()
            val zeroCall = callInAllRounds[0]

            log.trace { "Processing call #$callN: ${zeroCall.invocation.method.toStr()}" }

            repeat(zeroCall.matchers.size) { nMatcher ->
                val matcher = callInAllRounds.map { it.matchers[nMatcher] }.last()
                val signature = callInAllRounds.map { it.signaturePart[nMatcher] }.toList()

                if (matcher is CompositeMatcher<*>) {
                    compositeMatchers.add(callInAllRounds.map {
                        it.matchers[nMatcher] as CompositeMatcher<*>
                    })
                }

                matcherMap[signature] = matcher
            }

            log.trace { "Matcher map for ${zeroCall.invocation.method.toStr()}: $matcherMap" }

            val argMatchers = mutableListOf<Matcher<*>>()

            var allAny = false

            repeat(zeroCall.invocation.args.size) { nArgument ->
                val signature = callInAllRounds.map {
                    CallRecorderImpl.packRef(it.invocation.args[nArgument], gateway)
                }.toList()


                log.trace { "Signature for $nArgument argument of ${zeroCall.invocation.method.toStr()}: $signature" }

                val matcher = matcherMap.remove(signature)?.let {
                    if (nArgument == 0 && it is AllAnyMatcher) {
                        allAny = true
                        ConstantMatcher<Any>(true)
                    } else {
                        it
                    }
                } ?: if (allAny)
                    ConstantMatcher<Any>(true)
                else
                    EqMatcher(zeroCall.invocation.args[nArgument])

                argMatchers.add(matcher)
            }

            for (cmList in compositeMatchers) {
                val matcher = cmList.last()

                matcher.subMatchers = matcher.operandValues.withIndex().map { (nOp, _) ->
                    val signature = cmList.map {
                        packRef(it.operandValues[nOp], gateway)
                    }.toList()

                    log.trace { "Signature for $nOp operand of $matcher composite matcher: $signature" }

                    matcherMap.remove(signature)
                            ?: EqMatcher(matcher.operandValues[nOp])
                } as List<Matcher<Any?>>?
            }

            if (zeroCall.invocation.method.isSuspend()) {
                log.trace { "Suspend function found. Replacing continuation with any() matcher" }
                argMatchers[argMatchers.size - 1] = ConstantMatcher<Any>(true)
            }

            if (matcherMap.isNotEmpty()) {
                throw MockKException("Failed matching mocking signature for\n${zeroCall.invocation}\nleft matchers: ${matcherMap.values}")
            }

            val im = InvocationMatcher(
                    zeroCall.invocation.self,
                    zeroCall.invocation.self.toStr(),
                    zeroCall.invocation.method,
                    argMatchers.toList() as List<Matcher<Any>>)
            log.trace { "Built matcher: $im" }
            calls.add(MatchedCall(zeroCall.retType,
                    zeroCall.invocation, im,
                    childMocks.contains(InternalPlatform.ref(zeroCall.invocation.self))))
        }
        return calls
    }

    companion object {
        val log = Logger<SignatureMatcherDetector>()

        fun MethodDescription.isSuspend(): Boolean {
            val sz = paramTypes.size
            if (sz == 0) {
                return false
            }
            return paramTypes[sz - 1].isSubclassOf(Continuation::class)
        }

    }
}