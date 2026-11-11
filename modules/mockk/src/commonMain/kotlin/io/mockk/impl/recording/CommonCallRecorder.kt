package io.mockk.impl.recording

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.ExclusionParameters
import io.mockk.MockKGateway.MockFactory
import io.mockk.MockKGateway.VerificationAcknowledger
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.RecordedCall
import io.mockk.impl.instantiation.AbstractInstantiator
import io.mockk.impl.instantiation.AnyValueGenerator
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString
import io.mockk.impl.recording.states.AnsweringState
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
    val ack: VerificationAcknowledger,
) : CallRecorder {
    override val calls = mutableListOf<RecordedCall>()

    /**
     * Constructor placeholder mocks (see `anyConstructed<T>()`) requested during the
     * current session. Only a verification session consumes them: VerifyingState treats
     * a bare placeholder as an aggregate verification target. The list is reset when a
     * new session starts, when sessions are suspended/resumed and on reset.
     */
    val constructorPlaceholders = mutableListOf<Any>()

    var state: CallRecordingState = initialState(this)
    var childHinter = factories.childHinter()

    /**
     * Stubbing/verification sessions that were interrupted by a nested session starting
     * on this recorder. Nested sessions happen when a recorded block itself initializes
     * mocks (for example a lazy delegate evaluating `every { ... }` in its initializer,
     * issue #1377) or starts another `every`/`verify`/`exclude` block.
     *
     * A suspended session is resumed as soon as the nested session that replaced it is
     * fully finished: for stubbing that is when the answer opportunity is delivered,
     * for verification and exclusion when the evaluator performs its final cleanup via
     * [reset].
     */
    private val suspendedSessions = ArrayDeque<SuspendedSession>()

    private class SuspendedSession(
        val state: CallRecordingState,
        val calls: List<RecordedCall>,
        val constructorPlaceholders: List<Any>,
        val childHinter: ChildHinter,
    )

    private fun suspendOngoingSessionIfAny() {
        // AnsweringState is the idle state: no session is in progress, nothing to suspend.
        if (state is AnsweringState) return
        suspendedSessions.addLast(
            SuspendedSession(state, calls.toList(), constructorPlaceholders.toList(), childHinter),
        )
        calls.clear()
        constructorPlaceholders.clear()
        childHinter = factories.childHinter()
    }

    private fun resumeSuspendedSession(session: SuspendedSession) {
        calls.clear()
        calls.addAll(session.calls)
        constructorPlaceholders.clear()
        constructorPlaceholders.addAll(session.constructorPlaceholders)
        childHinter = session.childHinter
        state = session.state
    }

    private fun resumeSuspendedSessionIfNestedFinished() {
        if (state !is AnsweringState || suspendedSessions.isEmpty()) return
        resumeSuspendedSession(suspendedSessions.removeLast())
    }

    override fun startStubbing() {
        suspendOngoingSessionIfAny()
        state = factories.stubbingState(this)
        log.trace { "Starting stubbing" }
    }

    override fun startVerification(params: VerificationParameters) {
        suspendOngoingSessionIfAny()
        constructorPlaceholders.clear()
        state = factories.verifyingState(this, params)
        log.trace { "Starting verification" }
    }

    override fun startExclusion(params: ExclusionParameters) {
        suspendOngoingSessionIfAny()
        state = factories.exclusionState(this, params)
        log.trace { "Starting exclusion" }
    }

    override fun done() {
        state = state.recordingDone()
    }

    override fun round(
        n: Int,
        total: Int,
    ) = state.round(n, total)

    override fun nCalls() = state.nCalls()

    override fun <T : Any> matcher(
        matcher: Matcher<*>,
        cls: KClass<T>,
    ): T = state.matcher(matcher, cls)

    override fun call(invocation: Invocation) = state.call(invocation)

    override fun answerOpportunity(): MockKGateway.AnswerOpportunity<*> {
        val opportunity = state.answerOpportunity()
        resumeSuspendedSessionIfNestedFinished()
        return opportunity
    }

    override fun estimateCallRounds(): Int = state.estimateCallRounds()

    override fun wasNotCalled(list: List<Any>) = state.wasNotCalled(list)

    override fun constructorPlaceholder(mock: Any) {
        constructorPlaceholders.add(mock)
    }

    override fun hintNextReturnType(
        cls: KClass<*>,
        n: Int,
    ) = childHinter.hint(n, cls)

    override fun discardLastCallRound() = state.discardLastCallRound()

    override fun isLastCallReturnsNothing() = state.isLastCallReturnsNothing()

    /**
     * Cleanup at the end of a verification or exclusion session. When sessions are still
     * suspended, this cleanup belongs to a nested session: the interrupted parent session
     * is resumed instead of resetting the recorder.
     */
    override fun reset() {
        val parent = suspendedSessions.removeLastOrNull()
        if (parent != null) {
            resumeSuspendedSession(parent)
        } else {
            resetToIdle()
        }
    }

    /**
     * Takes the ongoing session out of the recorder so that the coroutine scaffolding
     * running the recorded block does not record its own context lookups into it
     * (issue #1247). The session is restored by [resumeSession].
     */
    override fun pauseSession() {
        suspendOngoingSessionIfAny()
        state = factories.answeringState(this)
    }

    /**
     * Restores a session paused by [pauseSession]. If the paused session was already
     * resumed (or there is none), this does nothing.
     */
    override fun resumeSession() {
        resumeSuspendedSessionIfNestedFinished()
    }

    /**
     * Drops the current session together with any suspended ones and returns the recorder
     * to the idle state. Used when a bad recording sequence makes the sessions unusable.
     */
    override fun cancelRecordingSession() {
        suspendedSessions.clear()
        resetToIdle()
    }

    private fun resetToIdle() {
        calls.clear()
        constructorPlaceholders.clear()
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
