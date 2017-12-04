package io.mockk.impl

import io.mockk.*
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.MockFactory
import io.mockk.impl.CallRecorderImpl.ChildHinter
import kotlin.reflect.KClass

internal abstract class CallRecorderState(val recorder: CallRecorderImpl) {
    open fun call(invocation: Invocation): Any? = cancelAndThrowBadRecordingState()
    open fun startStubbing(): CallRecorderState = cancelAndThrowBadRecordingState()
    open fun startVerification(): CallRecorderState = cancelAndThrowBadRecordingState()
    open fun catchArgs(round: Int, n: Int): Unit = cancelAndThrowBadRecordingState()
    open fun answer(answer: Answer<*>): Unit = cancelAndThrowBadRecordingState()
    open fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = cancelAndThrowBadRecordingState()
    open fun doneVerification(): CallRecorderState = cancelAndThrowBadRecordingState()
    open fun nCalls(): Int = cancelAndThrowBadRecordingState()
    open fun estimateCallRounds(): Int = cancelAndThrowBadRecordingState()

    private fun cancelAndThrowBadRecordingState(): Nothing {
        recorder.reset()
        throw MockKException("Bad recording sequence. State: ${recorder.state::class}")
    }
}


internal class AnsweringCallRecorderState(recorder: CallRecorderImpl) : CallRecorderState(recorder) {
    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        stub.recordCall(invocation.copy(originalCall = { null }))
        val answer = stub.answer(invocation)
        CallRecorderImpl.log.debug { "Recorded call: $invocation, answer: ${answerToString(answer)}" }
        return answer
    }

    override fun startStubbing() = StubbingCallRecorderState(recorder)
    override fun startVerification() = VerifyingCallRecorderState(recorder)

    private fun answerToString(answer: Any?) = recorder.stubRepo[answer]?.toStr() ?: answer.toString()
}

internal abstract class RecordingCallRecorderState(recorder: CallRecorderImpl) : CallRecorderState(recorder) {
    val childMocks = mutableListOf<Ref>()
    val temporaryMocks = mutableMapOf<KClass<*>, Any>()

    private val signedCalls = mutableListOf<SignedCall>()
    private val callRounds = mutableListOf<CallRound>()
    private val matchers = mutableListOf<Matcher<*>>()
    private val signatures = mutableListOf<Any>()

    override fun catchArgs(round: Int, n: Int) {
        if (round > 0) {
            callRounds.add(CallRound(signedCalls.toList()))
        }

        signedCalls.clear()
        recorder.childHinter = ChildHinter()

        if (round == n) {
            try {
                signMatchers()
                mockRealChilds()
            } finally {
                callRounds.clear()
            }
        }
    }

    private fun signMatchers() {
        recorder.calls.clear()
        val detector = SignatureMatcherDetector(callRounds, childMocks)
        recorder.calls.addAll(detector.detect())
    }

    override fun call(invocation: Invocation): Any? {
        if (childMocks.any { mock -> invocation.args.any { it === mock } }) {
            throw MockKException("Passing child mocks to arguments is prohibited")
        }
        val retType = recorder.childHinter.nextChildType { invocation.method.returnType }

        signedCalls.add(SignedCall(retType, invocation, matchers.toList(), signatures.toList()))
        matchers.clear()
        signatures.clear()

        return recorder.anyValueGenerator.anyValue(retType) {
            try {
                val mock = temporaryMocks[retType]
                if (mock != null) {
                    return@anyValue mock
                }

                val child = recorder.mockFactory.childMock(retType)

                childMocks.add(InternalPlatform.ref(child))

                temporaryMocks[retType] = child

                child
            } catch (ex: MockKException) {
                CallRecorderImpl.log.trace(ex) { "Returning 'null' for a final class assuming it is last in a call chain" }
                null
            }
        }
    }

    fun mockRealChilds() {
        var newSelf: Any? = null
        val newCalls = mutableListOf<MatchedCall>()

        for ((idx, ic) in recorder.calls.withIndex()) {
            val lastCall = idx == recorder.calls.size - 1

            val invocation = ic.invocation

            if (!ic.chained) {
                newSelf = invocation.self
            }

            val newInvocation = ic.invocation.copy(self = newSelf!!)
            val newMatcher = ic.matcher.copy(self = newSelf)
            val newCall = ic.copy(invocation = newInvocation, matcher = newMatcher)

            newCalls.add(newCall)

            if (!lastCall && recorder.calls[idx + 1].chained) {

                val args = newCall.matcher.args.map {
                    when (it) {
                        is EquivalentMatcher -> it.equivalent()
                        else -> it
                    }
                }
                val matcher = newCall.matcher.copy(args = args)
                val equivalentCall = newCall.copy(matcher = matcher)

                CallRecorderImpl.log.trace { "Child search key: $matcher" }

                newSelf = recorder.stubRepo.stubFor(newSelf).childMockK(equivalentCall)
            }
        }

        recorder.calls.clear()
        recorder.calls.addAll(newCalls)

        CallRecorderImpl.log.trace { "Mocked childs" }
    }

    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T {
        matchers.add(matcher)
        val signatureValue = recorder.signatureValueGenerator.signatureValue(cls) {
            recorder.instantiator.instantiate(cls)
        }
        signatures.add(InternalPlatform.packRef(signatureValue)!!)
        return signatureValue
    }

    override fun nCalls(): Int = signedCalls.size

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
}

internal class VerifyingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun doneVerification() = AnsweringCallRecorderState(recorder)
}

internal class StubbingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun catchArgs(round: Int, n: Int) {
        super.catchArgs(round, n)
        if (round == n) {
            recorder.state = StubbingAwaitingAnswerCallRecorderState(recorder)
        }
    }
}

internal class StubbingAwaitingAnswerCallRecorderState(recorder: CallRecorderImpl) : CallRecorderState(recorder) {
    override fun answer(answer: Answer<*>) {
        for ((idx, ic) in recorder.calls.withIndex()) {
            val lastCall = idx == recorder.calls.size - 1

            val ans = if (lastCall) {
                answer
            } else {
                ConstantAnswer(recorder.calls[idx + 1].invocation.self)
            }

            recorder.stubRepo.stubFor(ic.invocation.self).addAnswer(ic.matcher, ans)
        }

        recorder.calls.clear()

        CallRecorderImpl.log.trace { "Done stubbing" }

        recorder.state = AnsweringCallRecorderState(recorder)
    }
}

internal class CallRecorderImpl(val stubRepo: StubRepository,
                                val instantiator: Instantiator,
                                val signatureValueGenerator: SignatureValueGenerator,
                                val mockFactory: MockFactory,
                                val anyValueGenerator: AnyValueGenerator) : CallRecorder {

    override val calls = mutableListOf<MatchedCall>()
    internal var state: CallRecorderState = AnsweringCallRecorderState(this)
    internal var childHinter = ChildHinter()

    class ChildHinter {
        private var childTypes = mutableMapOf<Int, KClass<*>>()

        fun nextChildType(defaultReturnType: () -> KClass<*>): KClass<*> {
            val type = childTypes[1]
            shift()
            return type ?: defaultReturnType()
        }

        private fun shift() {
            childTypes = childTypes
                    .mapKeys { (k, _) -> k - 1 }
                    .filter { (k, _) -> k > 0 }
                    .toMutableMap()
        }

        fun hint(n: Int, cls: KClass<*>) {
            childTypes[n] = cls
        }
    }

    override fun startStubbing() {
        state = state.startStubbing()
        log.trace { "Starting stubbing" }
    }

    override fun startVerification() {
        state = state.startVerification()
        log.trace { "Starting verification" }
    }

    override fun catchArgs(round: Int, n: Int) = state.catchArgs(round, n)
    override fun nCalls() = state.nCalls()
    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = state.matcher(matcher, cls)
    override fun call(invocation: Invocation) = state.call(invocation)
    override fun answer(answer: Answer<*>) = state.answer(answer)
    override fun hintNextReturnType(cls: KClass<*>, n: Int) = childHinter.hint(n, cls)
    override fun estimateCallRounds(): Int = state.estimateCallRounds()

    override fun doneVerification() {
        state = state.doneVerification()
    }

    override fun reset() {
        calls.clear()
        childHinter = ChildHinter()
        state = AnsweringCallRecorderState(this)
    }

    companion object {
        val log = Logger<CallRecorderImpl>()
    }
}

