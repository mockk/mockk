package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.*
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import io.mockk.InternalPlatform.toStr

private data class SignedCall(val retType: KClass<*>,
                              val invocation: Invocation,
                              val matchers: List<Matcher<*>>,
                              val signaturePart: List<Any>)

private data class CallRound(val calls: List<SignedCall>)

internal class CallRecorderImpl(private val gateway: MockKGatewayImpl) : CallRecorder {
    private enum class Mode {
        STUBBING, STUBBING_WAITING_ANSWER, VERIFYING, ANSWERING
    }

    private var mode = Mode.ANSWERING

    private val signedCalls = mutableListOf<SignedCall>()
    private val callRounds = mutableListOf<CallRound>()
    override val calls = mutableListOf<MatchedCall>()
    private val childMocks = mutableListOf<Ref>()
    private val temporaryMocks = mutableMapOf<KClass<*>, Any>()
    private var childTypes = mutableMapOf<Int, KClass<*>>()

    private val matchers = mutableListOf<Matcher<*>>()
    private val signatures = mutableListOf<Any>()

    private fun checkMode(vararg modes: Mode) {
        if (!modes.any { it == mode }) {
            if (mode == Mode.STUBBING_WAITING_ANSWER) {
                cancel()
                throw MockKException("Bad recording sequence. Finish every/coEvery with returns/answers/throws/just Runs")
            }
            cancel()
            throw MockKException("Bad recording sequence. Mode: $mode")
        }
    }

    override fun startStubbing() {
        log.trace { "Starting stubbing" }
        checkMode(Mode.ANSWERING)
        mode = Mode.STUBBING
        childMocks.clear()
        temporaryMocks.clear()
    }

    override fun startVerification() {
        log.trace { "Starting verification" }
        checkMode(Mode.ANSWERING)
        mode = Mode.VERIFYING
        childMocks.clear()
        temporaryMocks.clear()
    }

    override fun catchArgs(round: Int, n: Int) {
        checkMode(Mode.STUBBING, Mode.VERIFYING)
        if (round > 0) {
            callRounds.add(CallRound(signedCalls.toList()))
        }
        signedCalls.clear()
        childTypes.clear()
        if (round == n) {
            try {
                signMatchers()
                mockRealChilds()
            } finally {
                callRounds.clear()
            }
            if (mode == Mode.STUBBING) {
                mode = Mode.STUBBING_WAITING_ANSWER
            }
        }
    }

    override fun nCalls() = signedCalls.size

    private fun signMatchers() {
        val detector = SignatureMatcherDetector()
        calls.clear()
        calls.addAll(detector.detect(callRounds, childMocks, gateway))

        childMocks.clear()
        temporaryMocks.clear()
    }

    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
        checkMode(Mode.STUBBING, Mode.VERIFYING)
        matchers.add(matcher)
        val signatureValue = gateway.instantiator.signatureValue(cls)
        signatures.add(packRef(signatureValue, gateway)!!)
        return signatureValue
    }

    override fun call(invocation: Invocation): Any? {
        if (mode == Mode.ANSWERING) {
            val stub = gateway.stubFor(invocation.self)
            stub.recordCall(invocation.copy(originalCall = { null }))
            val answer = stub.answer(invocation)
            log.debug { "Recorded call: $invocation, answer: ${answerToString(answer)}" }
            return answer
        } else {
            return addCallWithMatchers(invocation)
        }
    }

    private fun answerToString(answer: Any?) = gateway.stubs[answer]?.toStr() ?: answer.toString()

    private fun addCallWithMatchers(invocation: Invocation): Any? {
        if (childMocks.any { mock -> invocation.args.any { it === mock } }) {
            throw MockKException("Passing child mocks to arguments is prohibited")
        }
        val retType = nextChildType { invocation.method.returnType }

        signedCalls.add(SignedCall(retType, invocation, matchers.toList(), signatures.toList()))
        matchers.clear()
        signatures.clear()

        return gateway.instantiator.anyValue(retType) {
            try {
                val mock = temporaryMocks[retType]
                if (mock != null) {
                    return@anyValue mock
                }

                val child = gateway.instantiator.proxy(retType,
                        false,
                        true,
                        moreInterfaces = arrayOf(),
                        stub = MockKStub(retType, "temporary mock"))
                childMocks.add(InternalPlatform.ref(child))

                temporaryMocks[retType] = child

                child
            } catch (ex: MockKException) {
                log.trace(ex) { "Returning 'null' for a final class assuming it is last in a call chain" }
                null
            }
        }
    }


    fun mockRealChilds() {
        var newSelf: Any? = null
        val newCalls = mutableListOf<MatchedCall>()

        for ((idx, ic) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val invocation = ic.invocation

            if (!ic.chained) {
                newSelf = invocation.self
            }

            val newInvocation = ic.invocation.copy(self = newSelf!!)
            val newMatcher = ic.matcher.copy(self = newSelf)
            val newCall = ic.copy(invocation = newInvocation, matcher = newMatcher)

            newCalls.add(newCall)

            if (!lastCall && calls[idx + 1].chained) {

                val args = newCall.matcher.args.map {
                    when (it) {
                        is EquivalentMatcher -> it.equivalent()
                        else -> it
                    }
                }
                val matcher = newCall.matcher.copy(args = args)
                val equivalentCall = newCall.copy(matcher = matcher)

                log.trace { "Child search key: $matcher" }

                newSelf = gateway.stubFor(newSelf).childMockK(equivalentCall)
            }
        }

        calls.clear()
        calls.addAll(newCalls)

        log.trace { "Mocked childs" }
    }

    override fun answer(answer: Answer<*>) {
        checkMode(Mode.STUBBING_WAITING_ANSWER)

        for ((idx, ic) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else {
                ConstantAnswer(calls[idx + 1].invocation.self)
            }

            gateway.stubFor(ic.invocation.self).addAnswer(ic.matcher, ans)
        }

        calls.clear()

        log.trace { "Done stubbing" }
        mode = Mode.ANSWERING
    }

    override fun doneVerification() {
        checkMode(Mode.VERIFYING)
        mode = Mode.ANSWERING
    }


    override fun cancel() {
        signedCalls.clear()
        callRounds.clear()
        calls.clear()
        childMocks.clear()
        temporaryMocks.clear()
        childTypes.clear()
        matchers.clear()
        signatures.clear()

        mode = Mode.ANSWERING
    }

    private fun nextChildType(defaultReturnType: () -> KClass<*>): KClass<*> {
        val type = childTypes[1]

        childTypes = childTypes
                .mapKeys { (k, _) -> k - 1 }
                .filter { (k, _) -> k > 0 }
                .toMutableMap()

        return type ?: defaultReturnType()
    }

    override fun hintNextReturnType(cls: KClass<*>, n: Int) {
        childTypes[n] = cls
    }

    /**
     * Main idea is to have enough random information
     * to create signature for the argument.
     *
     * Max 40 calls looks like reasonable compromise
     */
    override fun estimateCallRounds(): Int {
        return signedCalls
                .flatMap { it.invocation.args }
                .filterNotNull()
                .map {
                    when (it::class) {
                        Boolean::class -> 40
                        Byte::class -> 8
                        Char::class -> 4
                        Short::class -> 4
                        Int::class -> 2
                        Float::class -> 2
                        else -> 1
                    }
                }
                .max() ?: 1
    }

    companion object {
        val log = logger<CallRecorderImpl>()
    }
}

private class SignatureMatcherDetector {
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
                    packRef(it.invocation.args[nArgument], gateway)
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
        val log = logger<SignatureMatcherDetector>()
    }
}

internal open class CommonRecorder(val gateway: MockKGatewayImpl) {

    internal fun <T, S : MockKMatcherScope> record(scope: S,
                                                   mockBlock: (S.() -> T)?,
                                                   coMockBlock: (suspend S.() -> T)?) {
        try {
            val callRecorder = gateway.callRecorder

            val block: () -> T = if (mockBlock != null) {
                { scope.mockBlock() }
            } else if (coMockBlock != null) {
                { runBlocking { scope.coMockBlock() } }
            } else {
                { throw MockKException("You should specify either 'mockBlock' or 'coMockBlock'") }
            }

            var childTypes = mutableMapOf<Int, KClass<*>>()
            callRecorder.autoHint(childTypes,0, 64, block)
            val n = callRecorder.estimateCallRounds();
            for (i in 1 until n) {
                callRecorder.autoHint(childTypes, i, n, block)
            }
            callRecorder.catchArgs(n, n)

        } catch (ex: ClassCastException) {
            throw MockKException("Class cast exception. " +
                    "Probably type information was erased.\n" +
                    "In this case use `hint` before call to specify " +
                    "exact return type of a method. ", ex)
        }
    }

    private fun <T> CallRecorder.autoHint(childTypes: MutableMap<Int, KClass<*>>, i: Int, n: Int, block: () -> T) {
        var callsPassed = -1
        while (true) {
            catchArgs(i, n)
            childTypes.forEach { callN, cls ->
                hintNextReturnType(cls, callN)
            }
            try {
                block()
                break
            } catch (ex: ClassCastException) {
                val clsName = extractClassName(ex) ?: throw ex
                val nCalls = nCalls()
                if (nCalls <= callsPassed) {
                    throw ex
                }
                callsPassed = nCalls
                val cls = Class.forName(clsName).kotlin

                log.trace { "Auto hint for $nCalls-th call: $cls" }
                childTypes[nCalls] = cls
            }
        }
    }

    internal fun prettifyCoroutinesException(ex: NoClassDefFoundError): Throwable {
        return if (ex.message?.contains("kotlinx/coroutines/") ?: false) {
            MockKException("Add coroutines support artifact 'org.jetbrains.kotlinx:kotlinx-coroutines-core' to your project ")
        } else {
            ex
        }
    }

    fun extractClassName(ex: ClassCastException): String? {
        return cannotBeCastRegex.find(ex.message!!)?.groups?.get(1)?.value
    }

    companion object {
        val cannotBeCastRegex = Regex("cannot be cast to (.+)$")
        val log = logger<CommonRecorder>()
    }
}

private fun MethodDescription.isSuspend(): Boolean {
    val sz = paramTypes.size
    if (sz == 0) {
        return false
    }
    return paramTypes[sz - 1].isSubclassOf(Continuation::class)
}

private fun packRef(arg: Any?, gateway: MockKGatewayImpl): Any? {
    return if (arg == null || gateway.instantiator.isPassedByValue(arg::class))
        arg
    else
        InternalPlatform.ref(arg)
}
