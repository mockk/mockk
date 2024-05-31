package io.mockk.impl.recording

import io.mockk.*
import io.mockk.InternalPlatformDsl.toArray
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString
import kotlin.coroutines.Continuation

class ChainedCallDetector(safeToString: SafeToString) {
    val log = safeToString(Logger<ChainedCallDetector>())

    val argMatchers = mutableListOf<Matcher<*>>()

    lateinit var call: RecordedCall

    fun detect(
        callRounds: List<CallRound>,
        callN: Int,
        matcherMap: HashMap<List<Any>, Matcher<*>>
    ) {
        val callInAllRounds = callRounds.map { it.calls[callN] }
        val zeroCall = callInAllRounds[0]
        var allAny = false

        log.trace { "Processing call #$callN: ${zeroCall.method.toStr()}" }

        fun buildMatcher(isStart: Boolean, zeroCallValue: Any?, matcherBySignature: Matcher<*>?): Matcher<*> =
            if (matcherBySignature == null) {
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
        fun composeVarArgMatcher(matchers: List<Matcher<*>>): Matcher<*> {
            val idx = matchers.indexOfFirst { it is VarargMatcher<*> }

            val prefix = matchers.subList(0, idx) as List<Matcher<Any>>
            val postfix = matchers.subList(idx + 1, matchers.size) as List<Matcher<Any>>

            val matcher = matchers[idx] as VarargMatcher<*>
            return matcher.copy(prefix = prefix, postfix = postfix)
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

            return when (
                varArgMatchers.count { it is VarargMatcher<*> }
            ) {
                0 -> ArrayMatcher<Any>(varArgMatchers.map { it } as List<Matcher<Any>>)
                1 -> composeVarArgMatcher(varArgMatchers)
                else -> throw MockKException("using more then one vararg VarargMatcher in one expression is not possible: $varArgMatchers")
            }
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
        fun buildRecordedCall(): RecordedCall {
            fun SignedCall.isSuspend() = when {
                method.isSuspend -> true
                method.isFnCall -> args.lastOrNull()?.let {
                    Continuation::class.isInstance(it)
                } ?: false
                else -> false
            }

            if (zeroCall.isSuspend()) {
                log.trace { "Suspend function found. Replacing continuation with any() matcher" }
                argMatchers[argMatchers.size - 1] = ConstantMatcher<Any>(true)
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

        detectArgMatchers()
        call = buildRecordedCall()
    }

    companion object {
        fun eqOrNullMatcher(arg: Any?): Matcher<Any> =
            if (arg == null) {
                NullCheckMatcher(false)
            } else {
                EqMatcher(arg)
            }
    }
}
