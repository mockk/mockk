package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.AdditionalAnswerOpportunity

class StubbingAwaitingAnswerState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    override fun answer(answer: Answer<*>) {
        val calls = recorder.calls

        var answerOpportunity: AdditionalAnswerOpportunity? = null

        for ((idx, recordedCall) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else if (recordedCall.isRetValueMock) {
                ConstantAnswer(recordedCall.retValue)
            } else {
                continue
            }

            answerOpportunity = recorder.stubRepo.stubFor(recordedCall.matcher.self)
                .addAnswer(recordedCall.matcher, ans)
        }

        calls.clear()

        log.trace { "Done stubbing. Still accepting additional answers" }

        recorder.state = recorder.factories.answeringStillAcceptingAnswersState(recorder, answerOpportunity!!)
    }

    companion object {
        val log = Logger<StubbingAwaitingAnswerState>()
    }
}