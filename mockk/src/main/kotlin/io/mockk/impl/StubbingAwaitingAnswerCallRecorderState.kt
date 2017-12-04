package io.mockk.impl

import io.mockk.Answer
import io.mockk.ConstantAnswer

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