package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.ConstantAnswer
import io.mockk.MockKException
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class StubbingAwaitingAnswerCallRecorderState(recorder: CommonCallRecorder) : CallRecorderState(recorder) {
    override fun answer(answer: Answer<*>) {
        for ((idx, matchedCall) in recorder.calls.withIndex()) {
            val lastCall = idx == recorder.calls.size - 1

            val ans = if (lastCall) {
                answer
            } else {
                ConstantAnswer(recorder.calls[idx + 1].invocation.self)
            }

            recorder.stubRepo.stubFor(matchedCall.invocation.self)
                    .addAnswer(matchedCall.matcher, ans)
        }

        recorder.calls.clear()

        log.trace { "Done stubbing" }

        recorder.state = recorder.factories.answeringCallRecorderState(recorder)
    }

    companion object {
        val log = Logger<StubbingAwaitingAnswerCallRecorderState>()
    }
}