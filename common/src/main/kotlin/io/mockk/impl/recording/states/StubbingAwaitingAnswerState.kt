package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class StubbingAwaitingAnswerState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    override fun answer(answer: Answer<*>) {
        val calls = recorder.calls
        for ((idx, recordedCall) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else if (recordedCall.isRetValueMock) {
                ConstantAnswer(recordedCall.retValue)
            } else {
                continue
            }

            recorder.stubRepo.stubFor(recordedCall.matcher.self)
                    .addAnswer(recordedCall.matcher, ans)
        }


        calls.clear()

        log.trace { "Done stubbing" }

        recorder.state = recorder.factories.answeringCallRecorderState(recorder)
    }

    companion object {
        val log = Logger<StubbingAwaitingAnswerState>()
    }
}