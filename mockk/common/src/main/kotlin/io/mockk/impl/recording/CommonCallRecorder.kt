package io.mockk.impl.recording

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKGateway.*
import io.mockk.RecordedCall
import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString
import io.mockk.impl.recording.states.CallRecordingState
import io.mockk.impl.stub.StubRepository
import kotlin.reflect.KClass

class CommonCallRecorder(
    val stubRepo: StubRepository,
    val instantiator: AbstractInstantiator,
    val signatureValueGenerator: SignatureValueGenerator,
    val mockFactory: MockFactory,
    val anyValueGenerator: () -> AnyValueGenerator,
    val safeToString: SafeToString,
    val factories: CallRecorderFactories,
    val initialState: (CommonCallRecorder) -> CallRecordingState,
    val ack: VerificationAcknowledger
) : CallRecorder {

    override val calls = mutableListOf<RecordedCall>()
    var state: CallRecordingState = initialState(this)
    var childHinter = factories.childHinter()

    override fun startStubbing() {
        state = factories.stubbingState(this)
        log.trace { "Starting stubbing" }
    }

    override fun startVerification(params: VerificationParameters) {
        state = factories.verifyingState(this, params)
        log.trace { "Starting verification" }
    }

    override fun startExclusion(params: ExclusionParameters) {
        state = factories.exclusionState(this, params)
        log.trace { "Starting exclusion" }
    }

    override fun done() {
        state = state.recordingDone()
    }

    override fun round(n: Int, total: Int) = state.round(n, total)
    override fun nCalls() = state.nCalls()
    override fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = state.matcher(matcher, cls)
    override fun call(invocation: Invocation) = state.call(invocation)
    override fun answerOpportunity() = state.answerOpportunity()
    override fun estimateCallRounds(): Int = state.estimateCallRounds()
    override fun wasNotCalled(list: List<Any>) = state.wasNotCalled(list)

    override fun hintNextReturnType(cls: KClass<*>, n: Int) = childHinter.hint(n, cls)
    override fun discardLastCallRound() = state.discardLastCallRound()
    override fun isLastCallReturnsNothing() = state.isLastCallReturnsNothing()

    override fun reset() {
        calls.clear()
        childHinter = factories.childHinter()
        state = initialState(this)
    }

    fun <T> safeExec(block: () -> T): T {
        val prevState = state
        try {
            state = factories.safeLoggingState(this)
            return block()
        } finally {
            state = prevState
        }
    }

    companion object {
        val log = Logger<CommonCallRecorder>()
    }
}
