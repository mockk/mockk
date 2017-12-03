package io.mockk.impl

import io.mockk.*
import java.lang.AssertionError

internal class VerifierImpl(gateway: MockKGatewayImpl) : CommonRecorder(gateway), MockKGateway.Verifier {
    var wasNotCalledWasCalled = false

    override fun checkWasNotCalled(mocks: List<Any>) {
        wasNotCalledWasCalled = true
        val calledStubs = mutableListOf<Stub>()
        for (mock in mocks) {
            val stub = gateway.stubFor(mock)
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

    override fun verify(ordering: Ordering, inverse: Boolean,
                        atLeast: Int,
                        atMost: Int,
                        exactly: Int,
                        mockBlock: (MockKVerificationScope.() -> Unit)?,
                        coMockBlock: (suspend MockKVerificationScope.() -> Unit)?) {
        if (ordering != Ordering.UNORDERED) {
            if (atLeast != 1 || atMost != Int.MAX_VALUE || exactly != -1) {
                throw MockKException("atLeast, atMost, exactly is only allowed in unordered verify block")
            }
        }

        val callRecorder = gateway.callRecorder
        callRecorder.startVerification()

        val lambda = slot<Function<*>>()
        val scope = MockKVerificationScope(gateway, lambda)

        try {
            record(scope, mockBlock, coMockBlock)
        } catch (ex: NoClassDefFoundError) {
            callRecorder.cancel()
            throw prettifyCoroutinesException(ex)
        } catch (ex: Throwable) {
            callRecorder.cancel()
            throw ex
        } finally {
            checkMissingCalls()
            wasNotCalledWasCalled = false
            callRecorder.doneVerification()
        }

        try {
            val min = if (exactly != -1) exactly else atLeast
            val max = if (exactly != -1) exactly else atMost

            val outcome = gateway.verifier(ordering).verify(callRecorder.calls, min, max)

            log.trace { "Done verification. Outcome: $outcome" }

            failIfNotPassed(outcome, inverse)
        } catch (ex: Throwable) {
            callRecorder.cancel()
            throw ex
        } finally {
            gateway.callRecorder.cancel()
        }
    }

    private fun checkMissingCalls() {
        if (gateway.callRecorder.calls.isEmpty() && !wasNotCalledWasCalled) {
            throw MockKException("Missing calls inside verify { ... } block.")
        }
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

    companion object {
        val log = Logger<VerifierImpl>()
    }
}