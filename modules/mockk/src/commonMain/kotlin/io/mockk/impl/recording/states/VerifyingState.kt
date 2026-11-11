package io.mockk.impl.recording.states

import io.mockk.MockKDsl
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.Ordering
import io.mockk.RecordedCall
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

        val hasWasNotCalls = sorter.wasNotCalledCalls.isNotEmpty() || sorter.wasNotCalledCallResults.isNotEmpty()
        if (params.timeout > 0 && hasWasNotCalls) {
            // A later call still breaks `wasNot Called`, so it is only checked once the timeout has elapsed.
            Thread.sleep((timeoutEnd - System.currentTimeMillis()).coerceAtLeast(0))
        }
        checkWasNotCalled(sorter.wasNotCalledCalls, sorter.wasNotCalledCallResults)

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

    private fun checkWasNotCalled(
        mocks: List<RecordedCall>,
        callResults: List<RecordedCall>,
    ) {
        for (callResult in callResults) {
            checkCallWasNotCalled(callResult)
        }

        val mockTargets = mocks.map { it.matcher.self }
        val calledStubs = mutableListOf<Stub>()
        for (mock in mockTargets) {
            if (!MockKDsl.internalIsMockKMock(
                    mock = mock,
                    regular = true,
                    spy = true,
                    objectMock = true,
                    staticMock = true,
                    constructorMock = true,
                )
            ) {
                throw MockKException("was not can only be called on a mocked object")
            }
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

    /**
     * `mock.method(args) wasNot Called`: the receiver of `wasNot` is the result of a call
     * recorded inside the verify block, so the check applies to that call pattern and not
     * to the receiver object. Verifying it with zero allowed calls fails when the mock
     * recorded a matching invocation (issue #1290).
     */
    private fun checkCallWasNotCalled(call: RecordedCall) {
        val wasNotParams = params.copy(ordering = Ordering.UNORDERED, min = 0, max = 0)
        val verifier = recorder.factories.verifier(wasNotParams)

        val outcome =
            recorder.safeExec {
                verifier.verify(listOf(call), wasNotParams)
            }

        if (outcome is VerificationResult.Failure) {
            throw AssertionError(
                recorder.safeExec {
                    "Verification failed: ${outcome.message}"
                },
            )
        }
    }

    companion object {
        val log = Logger<VerifyingState>()
    }
}
