package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class AnsweringCallRecorderState(recorder: CommonCallRecorder) : CallRecorderState(recorder) {
    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        stub.recordCall(invocation.copy(originalCall = { null }))
        val answer = stub.answer(invocation)
        log.debug { "Recorded call: $invocation, answer: ${answerToString(answer)}" }
        return answer
    }

    override fun startStubbing() = recorder.factories.stubbingCallRecorderState(recorder)
    override fun startVerification(params: VerificationParameters) = recorder.factories.verifyingCallRecorderState(recorder, params)

    private fun answerToString(answer: Any?) =
            if (answer == null) "null"
            else recorder.stubRepo[answer]?.toStr() ?: answer.toString()

    companion object {
        val log = Logger<AnsweringCallRecorderState>()
    }
}