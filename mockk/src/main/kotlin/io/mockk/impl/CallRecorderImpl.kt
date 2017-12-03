package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.CallRecorder
import kotlin.coroutines.experimental.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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
        val signatureValue = gateway.signatureValueGenerator.signatureValue(cls) {
            gateway.instantiator.instantiate(cls)
        }
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

        return gateway.anyValueGenerator.anyValue(retType) {
            try {
                val mock = temporaryMocks[retType]
                if (mock != null) {
                    return@anyValue mock
                }

                val child = gateway.mockFactory.childMock(retType)

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
        val log = Logger<CallRecorderImpl>()

        fun packRef(arg: Any?, gateway: MockKGatewayImpl): Any? {
            return if (arg == null || gateway.instantiator.isPassedByValue(arg::class))
                arg
            else
                InternalPlatform.ref(arg)
        }
    }

}

