package io.mockk.impl

import io.mockk.*
import io.mockk.InternalPlatform.toStr
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.full.isSubclassOf

internal class ChainedCallDetector(val callRounds: List<CallRound>,
                                   val childMocks: List<Ref>,
                                   val callN: Int) {
    val callInAllRounds = callRounds.map { it.calls[callN] }
    val matcherMap = hashMapOf<List<Any>, Matcher<*>>()
    val compositeMatchers = mutableListOf<List<CompositeMatcher<*>>>()
    val zeroCall = callInAllRounds[0]
    val argMatchers = mutableListOf<Matcher<*>>()

    init {
        log.trace { "Processing call #$callN: ${zeroCall.invocation.method.toStr()}" }
    }

    fun gatherMatchers() {
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
    }

    fun detectArgMatchers() {
        var allAny = false
        repeat(zeroCall.invocation.args.size) { nArgument ->
            val signature = callInAllRounds.map {
                InternalPlatform.packRef(it.invocation.args[nArgument])
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
    }

    @Suppress("UNCHECKED_CAST")
    fun processCompositeMatchers() {
        for (cmList in compositeMatchers) {
            val matcher = cmList.last()

            matcher.subMatchers = matcher.operandValues.withIndex().map { (nOp, _) ->
                val signature = cmList.map {
                    InternalPlatform.packRef(it.operandValues[nOp])
                }.toList()

                log.trace { "Signature for $nOp operand of $matcher composite matcher: $signature" }

                matcherMap.remove(signature)
                        ?: EqMatcher(matcher.operandValues[nOp])
            } as List<Matcher<Any?>>?
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun buildChainedCall(): MatchedCall {
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

        return MatchedCall(zeroCall.retType,
                zeroCall.invocation, im,
                childMocks.contains(InternalPlatform.ref(zeroCall.invocation.self)))
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