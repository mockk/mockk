package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class AnsweringState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    val log = recorder.safeLog(Logger<AnsweringState>())

    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        stub.recordCall(invocation.copy(originalCall = { null }))
        val answer = stub.answer(invocation)
        log.info { "Answering ${answer.toStr()} on $invocation" }
        return answer
    }

    override fun startStubbing() = recorder.factories.stubbingCallRecorderState(recorder)
    override fun startVerification(params: VerificationParameters) = recorder.factories.verifyingCallRecorderState(recorder, params)
}