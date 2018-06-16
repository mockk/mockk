package io.mockk.impl.recording

import io.mockk.*
import io.mockk.InternalPlatformDsl.toArray
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeLog
import kotlin.reflect.KClass

class ChainedCallDetector(safeLog: SafeLog) {
    val log = safeLog(Logger<SignatureMatcherDetector>())

    val matcherMap = hashMapOf<List<Any>, Matcher<*>>()
    val allCompositeMatchers = mutableListOf<List<CompositeMatcher<*>>>()
    val argMatchers = mutableListOf<Matcher<*>>()

    lateinit var call: RecordedCall

    @Suppress("CAST_NEVER_SUCCEEDS")
    fun detect(callRounds: List<CallRound>, callN: Int) {
        val callInAllRounds = callRounds.map { it.calls[callN] }
        val zeroCall = callInAllRounds[0]
        var allAny = false

        log.trace { "Processing call #$callN: ${zeroCall.method.toStr()}" }

        fun gatherMatchers() {
            repeat(zeroCall.matchers.size) { nMatcher ->
                val matcher = callInAllRounds.map { it.matchers[nMatcher].matcher }.last()
                val signature = callInAllRounds.map { it.matchers[nMatcher].signature }.toList()

                if (matcher is CompositeMatcher<*>) {
                    allCompositeMatchers.add(callInAllRounds.map {
                        it.matchers[nMatcher].matcher as CompositeMatcher<*>
                    })
                }

                matcherMap[signature] = matcher
            }

            log.trace { "Matcher map for ${zeroCall.method.toStr()}: $matcherMap" }
        }

        fun buildMatcher(isStart: Boolean, zeroCallValue: Any?, matcherBySignature: Matcher<*>?): Matcher<*> {
            return if (matcherBySignature == null) {
                if (allAny)
                    ConstantMatcher(true)
                else {
                    eqOrNullMatcher(zeroCallValue)
                }
            } else {
                if (isStart && matcherBySignature is AllAnyMatcher) {
                    allAny = true
                    ConstantMatcher<Any>(true)
                } else {
                    matcherBySignature
                }
            }
        }

        fun regularArgument(nArgument: Int): Matcher<*> {
            val signature = callInAllRounds.map {
                InternalPlatform.packRef(it.args[nArgument])
            }.toList()

            log.trace { "Signature for $nArgument argument of ${zeroCall.method.toStr()}: $signature" }

            val matcherBySignature = matcherMap.remove(signature)

            return buildMatcher(
                nArgument == 0,
                zeroCall.args[nArgument],
                matcherBySignature
            )
        }

        @Suppress("UNCHECKED_CAST")
        fun varArgArgument(nArgument: Int): Matcher<*> {
            val varArgMatchers = mutableListOf<Matcher<*>>()

            val zeroCallArg = zeroCall.args[nArgument]!!.toArray()
            repeat(zeroCallArg.size) { nVarArg ->
                val signature = callInAllRounds.map {
                    val arg = it.args[nArgument]!!.toArray()
                    InternalPlatform.packRef(arg[nVarArg])
                }.toList()

                log.trace { "Signature for $nArgument/$nVarArg argument of ${zeroCall.method.toStr()}: $signature" }

                val matcherBySignature = matcherMap.remove(signature)
                varArgMatchers.add(
                    buildMatcher(
                        nArgument == 0 && nVarArg == 0,
                        zeroCallArg[nVarArg],
                        matcherBySignature
                    )
                )
            }

            return ArrayMatcher<Any>(varArgMatchers.map { it as Matcher<*> } as List<Matcher<Any>>)
        }

        fun detectArgMatchers() {
            allAny = false

            val varArgsArg = zeroCall.method.varArgsArg

            repeat(zeroCall.args.size) { nArgument ->
                val matcher = if (varArgsArg == nArgument) {
                    varArgArgument(nArgument)
                } else {
                    regularArgument(nArgument)
                }

                argMatchers.add(matcher)
            }
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
                            ?: eqOrNullMatcher(matcher.operandValues[nOp])
                } as List<Matcher<Any?>>?
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun buildRecordedCall(): RecordedCall {
            if (zeroCall.method.isSuspend()) {
                log.trace { "Suspend function found. Replacing continuation with any() matcher" }
                argMatchers[argMatchers.size - 1] = ConstantMatcher<Any>(true)
            }

            if (matcherMap.isNotEmpty()) {
                throw MockKException("Failed matching mocking signature for\n${zeroCall.invocationStr}\nleft matchers: ${matcherMap.values}")
            }

            val im = InvocationMatcher(
                zeroCall.self,
                zeroCall.method,
                argMatchers.toList() as List<Matcher<Any>>,
                allAny
            )
            log.trace { "Built matcher: $im" }

            return RecordedCall(
                zeroCall.retValue,
                zeroCall.isRetValueMock,
                zeroCall.retType,
                im,
                null,
                null
            )
        }

        gatherMatchers()
        detectArgMatchers()
        processCompositeMatchers()
        call = buildRecordedCall()
    }

    @Suppress("UNCHECKED_CAST")
    protected fun MethodDescription.isSuspend(): Boolean {
        return InternalPlatform.isSuspend(paramTypes as List<KClass<Any>>)
    }

    protected fun eqOrNullMatcher(arg: Any?): Matcher<Any> {
        return if (arg == null) {
            NullCheckMatcher(false)
        } else {
            EqMatcher(arg)
        }
    }

}