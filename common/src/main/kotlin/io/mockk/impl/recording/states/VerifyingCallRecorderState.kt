package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.RecordedCall
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.recording.WasNotCalled
import io.mockk.impl.stub.Stub

class VerifyingCallRecorderState(recorder: CommonCallRecorder,
                                 val params: VerificationParameters) : RecordingCallRecorderState(recorder) {

    override fun wasNotCalled(list: List<Any>) {
        addWasNotCalled(list)
    }

    override fun recordingDone(): CallRecorderState {
        checkMissingCalls()

        val verifier = recorder.factories.verifier(params.ordering)

        val sorter = recorder.factories.verificationCallSorter()

        sorter.sort(recorder.calls)

        val outcome =
                recorder.safeExec {
                    verifier.verify(
                            sorter.regularCalls,
                            params.min,
                            params.max)
                }

        if (outcome.matches) {
            verifier.captureArguments()
        }

        log.trace { "Done verification. Outcome: $outcome" }
        failIfNotPassed(outcome, params.inverse)

        checkWasNotCalled(sorter.wasNotCalledCalls)

        return recorder.factories.answeringCallRecorderState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside verify { ... } block.")
        }
    }

    private fun failIfNotPassed(outcome: VerificationResult, inverse: Boolean) {
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

    private fun checkWasNotCalled(wasNotCalledCalls: List<RecordedCall>) {
        val calledStubs = mutableListOf<Stub>()
        for (call in wasNotCalledCalls) {
            val stub = recorder.stubRepo.stubFor(call.matcher.self)
            val calls = stub.allRecordedCalls()
            if (calls.isNotEmpty()) {
                calledStubs += stub
            }
        }

        if (!calledStubs.isEmpty()) {
            if (calledStubs.size == 1) {
                throw AssertionError(recorder.safeExec {
                    "Verification failed: ${calledStubs[0].toStr()} was called:\n" +
                            calledStubs[0].allRecordedCalls().joinToString("\n")
                })
            } else {
                throw AssertionError(recorder.safeExec {
                    "Verification failed: ${calledStubs.map { it.toStr() }.joinToString(", ")} were called:\n" +
                            calledStubs.flatMap { it.allRecordedCalls() }.joinToString("\n")
                })
            }
        }
    }

    companion object {
        val log = Logger<VerifyingCallRecorderState>()
    }
}