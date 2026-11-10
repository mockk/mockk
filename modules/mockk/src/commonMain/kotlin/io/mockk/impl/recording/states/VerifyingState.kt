package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.Stub
import io.mockk.impl.verify.ConstructorAggregateVerifier
import io.mockk.impl.verify.TimeoutVerifier
import io.mockk.impl.verify.VerificationHelpers

class VerifyingState(
    recorder: CommonCallRecorder,
    val params: VerificationParameters,
) : RecordingState(recorder) {
    override fun wasNotCalled(list: List<Any>) = addWasNotCalled(list)

    override fun recordingDone(): CallRecordingState {
        val constructorPlaceholders = recorder.constructorPlaceholders.toList()

        // A bare `anyConstructed<T>()` placeholder records no call of its own. When nothing
        // else was recorded, it is verified as an aggregate target. Any recorded call
        // (including `wasNot Called` markers) keeps the regular verification path.
        val aggregateConstructorVerification =
            recorder.calls.isEmpty() &&
                constructorPlaceholders.isNotEmpty() &&
                params.min == 0 &&
                params.max == 0

        checkMissingCalls(constructorPlaceholders, aggregateConstructorVerification)

        val timeoutEnd = System.currentTimeMillis() + params.timeout

        val verifier =
            if (aggregateConstructorVerification) {
                timeoutAware(
                    ConstructorAggregateVerifier(
                        recorder.stubRepo,
                        MockKGateway.implementation().constructorMockFactory,
                        recorder.safeToString,
                        constructorPlaceholders,
                    ),
                )
            } else {
                recorder.factories.verifier(params)
            }

        val sorter = recorder.factories.verificationCallSorter()

        sorter.sort(recorder.calls)

        val outcome =
            recorder.safeExec {
                verifier.verify(
                    sorter.regularCalls,
                    params,
                )
            }

        if (outcome.matches) {
            verifier.captureArguments()
        }

        log.trace { "Done verification. Outcome: $outcome" }
        failIfNotPassed(outcome, params.inverse)
        markVerified(outcome)

        val wasNotCalledMocks = sorter.wasNotCalledCalls.map { it.matcher.self }
        if (params.timeout > 0 && wasNotCalledMocks.isNotEmpty()) {
            // A later call still breaks `wasNot Called`, so it is only checked once the timeout has elapsed.
            Thread.sleep((timeoutEnd - System.currentTimeMillis()).coerceAtLeast(0))
        }
        checkWasNotCalled(wasNotCalledMocks)

        return recorder.factories.answeringState(recorder)
    }

    /**
     * A bare `anyConstructed<T>()` placeholder records no call, so an empty block is
     * expected when the placeholder is verified as an aggregate (`exactly = 0`).
     * In every other case an empty block is an error.
     */
    private fun checkMissingCalls(
        constructorPlaceholders: List<Any>,
        aggregateConstructorVerification: Boolean,
    ) {
        if (recorder.calls.isEmpty() && !aggregateConstructorVerification) {
            val placeholderHint =
                if (constructorPlaceholders.isNotEmpty()) {
                    "\n\nA bare anyConstructed<T>() placeholder can only be verified with exactly = 0."
                } else {
                    ""
                }
            throw MockKException("Missing calls inside verify { ... } block.$placeholderHint")
        }
    }

    /**
     * A verification with timeout waits for late constructions to happen, the same way
     * regular zero-call verifications do.
     */
    private fun timeoutAware(verifier: ConstructorAggregateVerifier): CallVerifier =
        if (params.timeout > 0) {
            TimeoutVerifier(recorder.stubRepo, verifier)
        } else {
            verifier
        }

    private fun failIfNotPassed(
        outcome: VerificationResult,
        inverse: Boolean,
    ) {
        when (outcome) {
            is VerificationResult.OK ->
                if (inverse) {
                    val callsReport = VerificationHelpers.formatCalls(outcome.verifiedCalls)
                    throw AssertionError("Inverse verification failed.\n\nVerified calls:\n$callsReport")
                }
            is VerificationResult.Failure ->
                if (!inverse) {
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
                throw AssertionError(
                    recorder.safeExec {
                        "Verification failed: ${calledStub.toStr()} should not be called:\n" +
                            calledStub.allRecordedCalls().joinToString("\n")
                    },
                )
            } else {
                throw AssertionError(
                    recorder.safeExec {
                        "Verification failed: ${calledStubs.joinToString(", ") { it.toStr() }} should not be called:\n" +
                            calledStubs.flatMap { it.allRecordedCalls() }.joinToString("\n")
                    },
                )
            }
        }
    }

    companion object {
        val log = Logger<VerifyingState>()
    }
}
