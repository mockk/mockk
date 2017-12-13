package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class StubbingAwaitingAnswerState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    override fun answer(answer: Answer<*>) {
        val calls = recorder.calls
        for ((idx, matchedCall) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else {
                ConstantAnswer(calls[idx + 1].matcher.self)
            }

            recorder.stubRepo.stubFor(matchedCall.matcher.self)
                    .addAnswer(matchedCall.matcher, ans)
        }


        calls.clear()

        log.trace { "Done stubbing" }

        recorder.state = recorder.factories.answeringCallRecorderState(recorder)
    }

    companion object {
        val log = Logger<StubbingAwaitingAnswerState>()
    }
}