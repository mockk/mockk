package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.Stub

class VerifyingState(
    recorder: CommonCallRecorder,
    val params: VerificationParameters
) : RecordingState(recorder) {

    override fun wasNotCalled(list: List<Any>) {
        addWasNotCalled(list)
    }

    override fun recordingDone(): CallRecordingState {
        checkMissingCalls()

        val verifier = recorder.factories.verifier(params.ordering)

        val sorter = recorder.factories.verificationCallSorter()

        sorter.sort(recorder.calls)

        val outcome =
            recorder.safeExec {
                verifier.verify(
                    sorter.regularCalls,
                    params.min,
                    params.max
                )
            }

        if (outcome.matches) {
            verifier.captureArguments()
        }

        log.trace { "Done verification. Outcome: $outcome" }
        failIfNotPassed(outcome, params.inverse)

        checkWasNotCalled(sorter.wasNotCalledCalls.map { it.matcher.self })

        return recorder.factories.answeringState(recorder)
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

    private fun checkWasNotCalled(mocks: List<Any>) {
        val calledStubs = mutableListOf<Stub>()
        for (mock in mocks) {
            val stub = recorder.stubRepo.stubFor(mock)
            val calls = stub.allRecordedCalls()
            if (calls.isNotEmpty()) {
                calledStubs += stub
            }
        }

        if (!calledStubs.isEmpty()) {
            if (calledStubs.size == 1) {
                val calledStub = calledStubs[0]
                throw AssertionError(recorder.safeExec {
                    "Verification failed: ${calledStub.toStr()} should not be called:\n" +
                            calledStub.allRecordedCalls().joinToString("\n")
                })
            } else {
                throw AssertionError(recorder.safeExec {
                    "Verification failed: ${calledStubs.map { it.toStr() }.joinToString(", ")} should not be called:\n" +
                            calledStubs.flatMap { it.allRecordedCalls() }.joinToString("\n")
                })
            }
        }
    }

    companion object {
        val log = Logger<VerifyingState>()
    }
}