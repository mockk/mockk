package io.mockk.impl.recording.states

import io.mockk.Answer
import io.mockk.Invocation
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.AdditionalAnswerOpportunity

class AnsweringStillAcceptingAnswersState(
    recorder: CommonCallRecorder,
    val answerOpportunity: AdditionalAnswerOpportunity
) : AnsweringState(recorder) {
    override val log = recorder.safeLog(Logger<AnsweringStillAcceptingAnswersState>())

    override fun call(invocation: Invocation): Any? {
        val res = super.call(invocation)
        recorder.state = recorder.factories.answeringState(recorder)
        return res
    }

    override fun answer(answer: Answer<*>) {
        answerOpportunity.addAnswer(answer)
    }
}