package io.mockk.impl

import io.mockk.Invocation

internal class AnsweringCallRecorderState(recorder: CallRecorderImpl) : CallRecorderState(recorder) {
    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        stub.recordCall(invocation.copy(originalCall = { null }))
        val answer = stub.answer(invocation)
        CallRecorderImpl.log.debug { "Recorded call: $invocation, answer: ${answerToString(answer)}" }
        return answer
    }

    override fun startStubbing() = StubbingCallRecorderState(recorder)
    override fun startVerification() = VerifyingCallRecorderState(recorder)

    private fun answerToString(answer: Any?) = recorder.stubRepo[answer]?.toStr() ?: answer.toString()
}