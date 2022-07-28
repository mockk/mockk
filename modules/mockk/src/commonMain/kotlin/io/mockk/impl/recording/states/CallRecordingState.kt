package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKException
import io.mockk.MockKGateway.AnswerOpportunity
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.reflect.KClass

abstract class CallRecordingState(val recorder: CommonCallRecorder) {
    open fun call(invocation: Invocation): Any? = cancelAndThrowBadRecordingState()
    open fun startStubbing(): CallRecordingState = cancelAndThrowBadRecordingState()
    open fun startVerification(params: VerificationParameters): CallRecordingState = cancelAndThrowBadRecordingState()
    open fun round(round: Int, total: Int): Unit = cancelAndThrowBadRecordingState()
    open fun answerOpportunity(): AnswerOpportunity<*> = cancelAndThrowBadRecordingState()
    open fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = cancelAndThrowBadRecordingState()
    open fun recordingDone(): CallRecordingState = cancelAndThrowBadRecordingState()
    open fun nCalls(): Int = cancelAndThrowBadRecordingState()
    open fun estimateCallRounds(): Int = cancelAndThrowBadRecordingState()
    open fun wasNotCalled(list: List<Any>): Unit = cancelAndThrowBadRecordingState()
    open fun discardLastCallRound(): Unit = cancelAndThrowBadRecordingState()
    open fun isLastCallReturnsNothing(): Boolean = cancelAndThrowBadRecordingState()


    private fun cancelAndThrowBadRecordingState(): Nothing {
        val state = recorder.state
        recorder.reset()
        if (state is StubbingAwaitingAnswerState) {
            throw MockKException("Bad recording sequence. Please finalize every { ... } block with returns/answers/just Runs")
        } else {
            throw MockKException("Bad recording sequence. State: ${state::class.simpleName}")
        }
    }

}