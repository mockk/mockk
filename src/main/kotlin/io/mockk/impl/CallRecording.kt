package io.mockk.impl

import io.mockk.*
import io.mockk.external.logger
import javassist.util.proxy.ProxyObject
import java.lang.AssertionError
import java.lang.reflect.Method
import kotlin.coroutines.experimental.Continuation

private data class SignedCall(val retType: Class<*>,
                              val invocation: Invocation,
                              val matchers: List<Matcher<*>>,
                              val signaturePart: List<Any>)

private data class CallRound(val calls: List<SignedCall>)

private fun Invocation.self() = self as MockKInstance

internal class CallRecorderImpl(private val gw: MockKGatewayImpl) : CallRecorder {
    private val log = logger<CallRecorderImpl>()

    private enum class Mode {
        STUBBING, VERIFYING, ANSWERING
    }

    private var mode = Mode.ANSWERING

    private val signedCalls = mutableListOf<SignedCall>()
    private val callRounds = mutableListOf<CallRound>()
    private val calls = mutableListOf<Call>()
    private val childMocks = mutableListOf<Ref>()
    private var childTypes = mutableMapOf<Int, Class<*>>()

    val matchers = mutableListOf<Matcher<*>>()
    val signatures = mutableListOf<Any>()

    private fun checkMode(vararg modes: Mode) {
        if (!modes.any { it == mode }) {
            throw MockKException("Bad recording sequence. Mode: $mode")
        }
    }

    override fun startStubbing() {
        log.info { "Starting stubbing" }
        checkMode(Mode.ANSWERING)
        mode = Mode.STUBBING
        childMocks.clear()
    }

    override fun startVerification() {
        log.info { "Starting verification" }
        checkMode(Mode.ANSWERING)
        mode = Mode.VERIFYING
        childMocks.clear()
    }

    override fun catchArgs(round: Int, n: Int) {
        checkMode(Mode.STUBBING, Mode.VERIFYING)
        if (round > 0) {
            callRounds.add(CallRound(signedCalls.toList()))
            signedCalls.clear()
            childTypes.clear()
        }
        if (round == n) {
            signMatchers()
            mockRealChilds()
            callRounds.clear()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun signMatchers() {
        val nCalls = callRounds[0].calls.size
        if (nCalls == 0) {
            throw MockKException("No calls inside every/verify {} block")
        }
        if (callRounds.any { it.calls.size != nCalls }) {
            throw MockKException("Not all call rounds result in same amount of calls")
        }

        calls.clear()

        repeat(nCalls) { callN ->

            val callInAllRounds = callRounds.map { it.calls[callN] }
            val matcherMap = hashMapOf<List<Any>, Matcher<*>>()
            val compositeMatchers = mutableListOf<List<CompositeMatcher<*>>>()
            val zeroCall = callInAllRounds[0]

            log.info { "Processing call #${callN}: ${zeroCall.invocation.method.toStr()}" }

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

            log.debug { "Matcher map for ${zeroCall.invocation.method.toStr()}: $matcherMap" }

            val argMatchers = mutableListOf<Matcher<*>>()

            var allAny = false

            repeat(zeroCall.invocation.args.size) { nArgument ->
                val signature = callInAllRounds.map {
                    packRef(it.invocation.args[nArgument])
                }.toList()


                log.debug { "Signature for $nArgument argument of ${zeroCall.invocation.method.toStr()}: $signature" }

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
                        packRef(it.operandValues[nOp])
                    }.toList()

                    log.debug { "Signature for $nOp operand of $matcher composite matcher: $signature" }

                    matcherMap.remove(signature)
                            ?: EqMatcher(matcher.operandValues[nOp])
                } as List<Matcher<Any?>>?
            }

            if (zeroCall.invocation.method.isSuspend()) {
                log.debug { "Suspend function found. Replacing continuation with any() matcher" }
                argMatchers[argMatchers.size - 1] = ConstantMatcher<Any>(true)
            }

            if (matcherMap.isNotEmpty()) {
                throw MockKException("Failed to find few matchers by signature: $matcherMap")
            }

            val im = InvocationMatcher(
                    EqMatcher(zeroCall.invocation.self, ref = true),
                    EqMatcher(zeroCall.invocation.method),
                    argMatchers.toList() as List<Matcher<Any>>)
            log.info { "Built matcher: $im" }
            calls.add(Call(zeroCall.retType,
                    zeroCall.invocation, im,
                    childMocks.contains(Ref(zeroCall.invocation.self))))
        }
        childMocks.clear()
    }

    private fun packRef(arg: Any?): Any? {
        return if (arg == null || gw.instantiator.isPassedByValue(arg.javaClass))
            arg
        else
            Ref(arg)
    }

    override fun <T> matcher(matcher: Matcher<*>, cls: Class<T>): T {
        checkMode(Mode.STUBBING, Mode.VERIFYING)
        matchers.add(matcher)
        val signatureValue = gw.instantiator.signatureValue(cls)
        signatures.add(packRef(signatureValue)!!)
        return signatureValue
    }

    override fun call(invocation: Invocation): Any? {
        if (mode == Mode.ANSWERING) {
            invocation.self().___recordCall(invocation)
            val answer = invocation.self().___answer(invocation)
            log.info { "Recorded call: $invocation, answer: $answer" }
            return answer
        } else {
            return addCallWithMatchers(invocation)
        }
    }

    private fun addCallWithMatchers(invocation: Invocation): Any? {
        if (childMocks.any { mock -> invocation.args.any { it === mock } }) {
            throw MockKException("Passing child mocks to arguments is prohibited")
        }

        val retType = nextChildType { invocation.method.returnType }

        signedCalls.add(SignedCall(retType, invocation, matchers.toList(), signatures.toList()))
        matchers.clear()
        signatures.clear()

        val instantiator = MockKGateway.LOCATOR().instantiator
        return instantiator.anyValue(retType) {
            val child = instantiator.proxy(retType, false) as MockK
            (child as ProxyObject).handler = MockKInstanceProxyHandler(retType, child)
            childMocks.add(Ref(child))
            child
        }
    }


    fun mockRealChilds() {
        var newSelf: MockKInstance? = null
        val newCalls = mutableListOf<Call>()

        for ((idx, ic) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val invocation = ic.invocation

            if (!ic.chained) {
                newSelf = invocation.self()
            }

            val newInvocation = ic.invocation.withSelf(newSelf!!)
            val newMatcher = ic.matcher.withSelf(EqMatcher(newSelf, ref = true))
            val newCall = ic.withInvocationAndMatcher(newInvocation, newMatcher)

            newCalls.add(newCall)

            if (!lastCall && calls[idx + 1].chained) {
                newSelf = newSelf.___childMockK(newCall)
            }
        }

        calls.clear()
        calls.addAll(newCalls)

        log.debug { "Mocked childs" }
    }

    override fun answer(answer: Answer<*>) {
        checkMode(Mode.STUBBING)

        for ((idx, ic) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else {
                ConstantAnswer(calls[idx + 1].invocation.self)
            }

            ic.invocation.self().___addAnswer(ic.matcher, ans)
        }

        calls.clear()

        log.debug { "Done stubbing" }
        mode = Mode.ANSWERING
    }

    override fun verify(ordering: Ordering, inverse: Boolean, min: Int, max: Int) {
        checkMode(Mode.VERIFYING)

        val outcome = gw.verifier(ordering).verify(calls, min, max)

        log.debug { "Done verification. Outcome: $outcome" }
        mode = Mode.ANSWERING

        failIfNotPassed(outcome, inverse)
    }

    private fun failIfNotPassed(outcome: VerificationResult, inverse: Boolean) {
        val matcherStr = if (outcome.matcher != null) ", matcher: ${outcome.matcher}" else ""

        if (inverse) {
            if (outcome.matches) {
                throw AssertionError("Inverse verification failed$matcherStr")
            }
        } else {
            if (!outcome.matches) {
                throw AssertionError("Verification failed$matcherStr")
            }
        }
    }

    private fun nextChildType(defaultReturnType: () -> Class<*>): Class<*> {
        val type = childTypes[1]

        childTypes = childTypes
                .mapKeys { (k, _) -> k - 1 }
                .filter { (k, _) -> k > 0 }
                .toMutableMap()

        return type ?: defaultReturnType()
    }

    override fun childType(cls: Class<*>, n: Int) {
        childTypes[n] = cls
    }
}

internal class UnorderedVerifierImpl(private val gw: MockKGatewayImpl) : Verifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        return calls
                .firstOrNull { !it.invocation.self().___matchesAnyRecordedCalls(it.matcher, min, max) }
                ?.matcher
                ?.let { VerificationResult(false, it) }
                ?: VerificationResult(true)
    }
}

private fun List<Call>.allCalls() =
        this.map { Ref(it.invocation.self) }
                .distinct()
                .map { it.value as MockKInstance }
                .flatMap { it.___allRecordedCalls() }
                .sortedBy { it.timestamp }

internal class OrderedVerifierImpl(private val gw: MockKGatewayImpl) : Verifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        val allCalls = calls.allCalls()

        if (calls.size > allCalls.size) {
            return VerificationResult(false)
        }

        // LCS algorithm
        var prev = Array(calls.size, { 0 })
        var curr = Array(calls.size, { 0 })
        for (call in allCalls) {
            for ((matcherIdx, matcher) in calls.map { it.matcher }.withIndex()) {
                curr[matcherIdx] = if (matcher.match(call)) {
                    if (matcherIdx == 0) 1 else prev[matcherIdx - 1] + 1
                } else {
                    maxOf(prev[matcherIdx], if (matcherIdx == 0) 0 else curr[matcherIdx - 1])
                }
            }
            val swap = curr
            curr = prev
            prev = swap
        }

        // match only if all matchers present
        return VerificationResult(prev.last() == calls.size)
    }
}

internal class SequenceVerifierImpl(private val gw: MockKGatewayImpl) : Verifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        val allCalls = calls.allCalls()

        if (allCalls.size != calls.size) {
            return VerificationResult(false)
        }

        for ((i, call) in allCalls.withIndex()) {
            if (!calls[i].matcher.match(call)) {
                return VerificationResult(false)
            }
        }

        return VerificationResult(true)
    }
}


private fun Method.isSuspend(): Boolean {
    if (parameterCount == 0) {
        return false
    }
    return Continuation::class.java.isAssignableFrom(parameterTypes[parameterCount - 1])
}
