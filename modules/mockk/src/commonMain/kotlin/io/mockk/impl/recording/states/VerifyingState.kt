package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.Stub
import io.mockk.impl.verify.VerificationHelpers

class VerifyingState(
    recorder: CommonCallRecorder,
    val params: VerificationParameters
) : RecordingState(recorder) {

    override fun wasNotCalled(list: List<Any>) = addWasNotCalled(list)

    override fun recordingDone(): CallRecordingState {
        checkMissingCalls()

        val verifier = recorder.factories.verifier(params)

        val sorter = recorder.factories.verificationCallSorter()

        sorter.sort(recorder.calls)

        val outcome =
            recorder.safeExec {
                verifier.verify(
                    sorter.regularCalls,
                    params
                )
            }

        if (outcome.matches) {
            verifier.captureArguments()
        }

        log.trace { "Done verification. Outcome: $outcome" }
        failIfNotPassed(outcome, params.inverse)
        markVerified(outcome)

        checkWasNotCalled(sorter.wasNotCalledCalls.map { it.matcher.self })

        return recorder.factories.answeringState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside verify { ... } block.")
        }
    }

    private fun failIfNotPassed(outcome: VerificationResult, inverse: Boolean) {
        when (outcome) {
            is VerificationResult.OK -> if (inverse) {
                val callsReport = VerificationHelpers.formatCalls(outcome.verifiedCalls)
                throw AssertionError("Inverse verification failed.\n\nVerified calls:\n$callsReport")
            }
            is VerificationResult.Failure -> if (!inverse) {
                throw AssertionError("Verification failed: ${outcome.message}")
            }
        }
    }

    private fun markVerified(outcome: VerificationResult) {
        if (outcome is VerificationResult.OK) {
            for (invocation in outcome.verifiedCalls) {
                recorder.ack.markCallVerified(invocation)
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

        if (calledStubs.isNotEmpty()) {
            if (calledStubs.size == 1) {
                val calledStub = calledStubs[0]
                throw AssertionError(recorder.safeExec {
                    "Verification failed: ${calledStub.toStr()} should not be called:\n" +
                        calledStub.allRecordedCalls().joinToString("\n")
                })
            } else {
                throw AssertionError(recorder.safeExec {
                    "Verification failed: ${calledStubs.joinToString(", ") { it.toStr() }} should not be called:\n" +
                        calledStubs.flatMap { it.allRecordedCalls() }.joinToString("\n")
                })
            }
        }
    }

    companion object {
        val log = Logger<VerifyingState>()
    }
}
