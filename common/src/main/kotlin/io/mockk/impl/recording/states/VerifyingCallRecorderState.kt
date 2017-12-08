package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.impl.stub.Stub
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder

class VerifyingCallRecorderState(recorder: CommonCallRecorder,
                                 val params: VerificationParameters) : RecordingCallRecorderState(recorder) {
    val wasNotCalled = mutableListOf<Any>()

    override fun recordingDone(): CallRecorderState {
        if (recorder.calls.isEmpty() && wasNotCalled.isEmpty()) {
            throw MockKException("Missing calls inside verify { ... } block.")
        }

        val outcome = recorder.factories.verifier(params.ordering)
                .verify(recorder.calls, params.min, params.max)

        log.trace { "Done verification. Outcome: $outcome" }
        failIfNotPassed(outcome, params.inverse)

        checkWasNotCalled()

        return recorder.factories.answeringCallRecorderState(recorder)
    }

    private fun failIfNotPassed(outcome: MockKGateway.VerificationResult, inverse: Boolean) {
        val explanation = if (outcome.message != null) ": ${outcome.message}" else ""

        if (inverse) {
            if (outcome.matches) {
                throw AssertionError("Inverse verification failed$explanation")
            }
        } else {
            if (!outcome.matches) {
                throw AssertionError("Verification failed$explanation")
            }
        }
    }

    private fun checkWasNotCalled() {
        val calledStubs = mutableListOf<Stub>()
        for (mock in wasNotCalled) {
            val stub = recorder.stubRepo.stubFor(mock)
            val calls = stub.allRecordedCalls()
            if (calls.isNotEmpty()) {
                calledStubs += stub
            }
        }

        if (!calledStubs.isEmpty()) {
            if (calledStubs.size == 1) {
                throw AssertionError("Verification failed: ${calledStubs[0]} was called")
            } else {
                throw AssertionError("Verification failed: $calledStubs were called")
            }
        }
    }

    override fun wasNotCalled(list: List<Any>) {
        wasNotCalled.addAll(list)
    }

    companion object {
        val log = Logger<VerifyingCallRecorderState>()
    }
}