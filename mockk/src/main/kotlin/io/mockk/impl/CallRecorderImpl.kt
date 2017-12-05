package io.mockk.impl

import io.mockk.Answer
import io.mockk.Invocation
import io.mockk.MatchedCall
import io.mockk.Matcher
import io.mockk.MockKGateway.*
import kotlin.reflect.KClass

internal class CallRecorderImpl(val stubRepo: StubRepository,
                                val instantiator: Instantiator,
                                val signatureValueGenerator: SignatureValueGenerator,
                                val mockFactory: MockFactory,
                                val anyValueGenerator: AnyValueGenerator,
                                val factories: CallRecorderFactories) : CallRecorder {

    override val calls = mutableListOf<MatchedCall>()
    internal var state: CallRecorderState = factories.answeringCallRecorderState(this)
    internal var childHinter = factories.childHinter()

    override fun startStubbing() {
        state = state.startStubbing()
        log.trace { "Starting stubbing" }
    }

    override fun startVerification(params: VerificationParameters) {
        state = state.startVerification(params)
        log.trace { "Starting verification" }
    }

    override fun catchArgs(round: Int, n: Int) = state.catchArgs(round, n)
    override fun nCalls() = state.nCalls()
    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = state.matcher(matcher, cls)
    override fun call(invocation: Invocation) = state.call(invocation)
    override fun answer(answer: Answer<*>) = state.answer(answer)
    override fun hintNextReturnType(cls: KClass<*>, n: Int) = childHinter.hint(n, cls)
    override fun estimateCallRounds(): Int = state.estimateCallRounds()
    override fun wasNotCalled(list: List<Any>) = state.wasNotCalled(list)

    override fun done() {
        state = state.recordingDone()
    }

    override fun reset() {
        calls.clear()
        childHinter = factories.childHinter()
        state = factories.answeringCallRecorderState(this)
    }

    companion object {
        val log = Logger<CallRecorderImpl>()
    }
}

