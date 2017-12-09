package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.MockKException
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.reflect.KClass

abstract class CallRecorderState(val recorder: CommonCallRecorder) {
    open fun call(invocation: Invocation): Any? = cancelAndThrowBadRecordingState()
    open fun startStubbing(): CallRecorderState = cancelAndThrowBadRecordingState()
    open fun startVerification(params: VerificationParameters): CallRecorderState = cancelAndThrowBadRecordingState()
    open fun catchArgs(round: Int, n: Int): Unit = cancelAndThrowBadRecordingState()
    open fun answer(answer: Answer<*>): Unit = cancelAndThrowBadRecordingState()
    open fun <T : Any> matcher(matcher: Matcher<*>, cls: KClass<T>): T = cancelAndThrowBadRecordingState()
    open fun recordingDone(): CallRecorderState = cancelAndThrowBadRecordingState()
    open fun nCalls(): Int = cancelAndThrowBadRecordingState()
    open fun estimateCallRounds(): Int = cancelAndThrowBadRecordingState()
    open fun wasNotCalled(list: List<Any>): Unit = cancelAndThrowBadRecordingState()

    private fun cancelAndThrowBadRecordingState(): Nothing {
        val state = recorder.state
        recorder.reset()
        if (state is StubbingAwaitingAnswerCallRecorderState) {
            throw MockKException("Bad recording sequence. Please finalize every { ... } block with returns/answers/just Runs")
        } else {
            throw MockKException("Bad recording sequence. State: ${state::class.simpleName}")
        }
    }
}