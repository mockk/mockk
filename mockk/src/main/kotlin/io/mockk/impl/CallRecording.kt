package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.*
import io.mockk.external.logger
import javassist.util.proxy.ProxyObject
import kotlinx.coroutines.experimental.runBlocking
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

private data class SignedCall(val retType: KClass<*>,
                              val invocation: Invocation,
                              val matchers: List<Matcher<*>>,
                              val signaturePart: List<Any>)

private data class CallRound(val calls: List<SignedCall>)

internal class CallRecorderImpl(private val gw: MockKGatewayImpl) : CallRecorder {
    private val log = logger<CallRecorderImpl>()

    private enum class Mode {
        STUBBING, STUBBING_WAITING_ANSWER, VERIFYING, ANSWERING
    }

    private var mode = Mode.ANSWERING

    private val signedCalls = mutableListOf<SignedCall>()
    private val callRounds = mutableListOf<CallRound>()
    override val calls = mutableListOf<Call>()
    private val childMocks = mutableListOf<Ref>()
    private var childTypes = mutableMapOf<Int, KClass<*>>()

    private val matchers = mutableListOf<Matcher<*>>()
    private val signatures = mutableListOf<Any>()

    private fun checkMode(vararg modes: Mode) {
        if (!modes.any { it == mode }) {
            if (mode == Mode.STUBBING_WAITING_ANSWER) {
                throw MockKException("Bad recording sequence. Finish every/coEvery with returns/answers/throws/just Runs")
            }
            throw MockKException("Bad recording sequence. Mode: $mode")
        }
    }

    override fun startStubbing() {
        log.trace { "Starting stubbing" }
        checkMode(Mode.ANSWERING)
        mode = Mode.STUBBING
        childMocks.clear()
    }

    override fun startVerification() {
        log.trace { "Starting verification" }
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

    private fun signMatchers() {
        val detector = SignatureMatcherDetector()
        calls.clear()
        calls.addAll(detector.detect(callRounds, childMocks))

        childMocks.clear()
    }

    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
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
            log.debug { "Recorded call: $invocation, answer: ${answer.toStr()}" }
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

        val instantiator = MockKGateway.implementation().instantiator
        return instantiator.anyValue(retType) {
            try {
                val child = instantiator.proxy(retType, false, moreInterfaces = arrayOf())
                if (child is MockK) {
                    (child as ProxyObject).handler = MockKInstanceProxyHandler(retType, "temporary mock", child)
                }
                childMocks.add(Ref(child))
                child
            } catch (ex: MockKException) {
                log.trace(ex) { "Returning 'null' for a final class assuming it is last in a call chain" }
                null
            }
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

            val newInvocation = ic.invocation.copy(self = newSelf!!)
            val newMatcher = ic.matcher.copy(self = newSelf)
            val newCall = ic.copy(invocation = newInvocation, matcher = newMatcher)

            newCalls.add(newCall)

            if (!lastCall && calls[idx + 1].chained) {
                newSelf = newSelf.___childMockK(newCall)
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

            ic.invocation.self().___addAnswer(ic.matcher, ans)
        }

        calls.clear()

        log.trace { "Done stubbing" }
        mode = Mode.ANSWERING
    }

    override fun doneVerification() {
        checkMode(Mode.VERIFYING)
        calls.clear()
        mode = Mode.ANSWERING
    }


    override fun cancel() {
        signedCalls.clear()
        callRounds.clear()
        calls.clear()
        childMocks.clear()
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
}

private class SignatureMatcherDetector {
    private val log = logger<SignatureMatcherDetector>()

    @Suppress("UNCHECKED_CAST")
    fun detect(callRounds: List<CallRound>, childMocks: List<Ref>) : List<Call> {
        val nCalls = callRounds[0].calls.size
        if (nCalls == 0) {
            throw MockKException("Missing calls inside every/verify {} block")
        }
        if (callRounds.any { it.calls.size != nCalls }) {
            throw MockKException("every/verify {} block were run several times. Recorded calls count differ between runs")
        }

        val calls = mutableListOf<Call>();

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
                    packRef(it.invocation.args[nArgument])
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
                        packRef(it.operandValues[nOp])
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
                    zeroCall.invocation.method,
                    argMatchers.toList() as List<Matcher<Any>>)
            log.trace { "Built matcher: $im" }
            calls.add(Call(zeroCall.retType,
                    zeroCall.invocation, im,
                    childMocks.contains(Ref(zeroCall.invocation.self))))
        }
        return calls
    }
}

internal open class CommonRecorder(val gateway: MockKGatewayImpl) {

    internal fun <T, S : MockKMatcherScope> record(scope: S,
                                                   mockBlock: (S.() -> T)?,
                                                   coMockBlock: (suspend S.() -> T)?) {
        try {
            if (mockBlock != null) {
                val n = MockKGatewayImpl.N_CALL_ROUNDS
                repeat(n) {
                    gateway.callRecorder.catchArgs(it, n)
                    scope.mockBlock()
                }
                gateway.callRecorder.catchArgs(n, n)
            } else if (coMockBlock != null) {
                runBlocking {
                    val n = MockKGatewayImpl.N_CALL_ROUNDS
                    repeat(n) {
                        gateway.callRecorder.catchArgs(it, n)
                        scope.coMockBlock()
                    }
                    gateway.callRecorder.catchArgs(n, n)
                }
            }
        } catch (ex: ClassCastException) {
            throw MockKException("Class cast exception. " +
                    "Probably type information was erased.\n" +
                    "In this case use `hint` before call to specify " +
                    "exact return type of a method. ", ex)
        }
    }

    internal fun prettifyCoroutinesException(ex: NoClassDefFoundError): Throwable {
        return if (ex.message?.contains("kotlinx/coroutines/") ?: false) {
            MockKException("Add coroutines support artifact 'org.jetbrains.kotlinx:kotlinx-coroutines-core' to your project ")
        } else {
            ex
        }
    }
}

private fun MethodDescription.isSuspend(): Boolean {
    val sz = paramTypes.size
    if (sz == 0) {
        return false
    }
    return paramTypes[sz - 1].isSubclassOf(Continuation::class)
}

private fun Invocation.self() = self as MockKInstance

private fun packRef(arg: Any?): Any? {
    return if (arg == null || MockKGateway.implementation().instantiator.isPassedByValue(arg::class))
        arg
    else
        Ref(arg)
}
