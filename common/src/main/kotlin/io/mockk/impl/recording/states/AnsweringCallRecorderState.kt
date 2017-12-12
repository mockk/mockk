package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.MockKException
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class AnsweringCallRecorderState(recorder: CommonCallRecorder) : CallRecorderState(recorder) {
    val log = recorder.safeLog(Logger<AnsweringCallRecorderState>())

    override fun call(invocation: Invocation): Any? {
        val stub = recorder.stubRepo.stubFor(invocation.self)
        stub.recordCall(invocation.copy(originalCall = { null }))
        val answer = stub.answer(invocation)
        log.info { "Answering $answer on $invocation" }
        return answer
    }

    override fun startStubbing() = recorder.factories.stubbingCallRecorderState(recorder)
    override fun startVerification(params: VerificationParameters) = recorder.factories.verifyingCallRecorderState(recorder, params)
}