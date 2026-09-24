package io.mockk.impl.verify

import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.StubRepository

class TimeoutVerifier(
    val stubRepo: StubRepository,
    val verifierChain: CallVerifier,
) : CallVerifier {
    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters,
    ): VerificationResult {
        if (params.inverse || params.max != Int.MAX_VALUE) {
            // A later call can still break an inverse or upper-bounded verification,
            // so it is only checked once the whole timeout has elapsed.
            Thread.sleep(params.timeout)
            return verifierChain.verify(verificationSequence, params).addTimeoutToMessage(params.timeout)
        }

        val stubs = verificationSequence.allStubs(stubRepo)

        val session = stubRepo.openRecordCallAwaitSession(stubs, params.timeout)
        try {
            while (true) {
                val result = verifierChain.verify(verificationSequence, params)
                if (params.inverse != result.matches) {
                    return result // passed
                }
                if (!session.wait()) {
                    val lastCheck = verifierChain.verify(verificationSequence, params)
                    if (params.inverse != lastCheck.matches) {
                        return lastCheck // passed
                    }
                    return lastCheck.addTimeoutToMessage(params.timeout)
                }
            }
        } finally {
            session.close()
        }
    }

    override fun captureArguments() = verifierChain.captureArguments()

    private fun List<RecordedCall>.allStubs(stubRepo: StubRepository) =
        this
            .map { InternalPlatform.ref(it.matcher.self) }
            .distinct()
            .map { it.value }
            .map { stubRepo.stubFor(it) }
            .distinct()
}

private fun VerificationResult.addTimeoutToMessage(timeout: Long) =
    when (this) {
        is VerificationResult.OK -> VerificationResult.OK(verifiedCalls)
        is VerificationResult.Failure -> VerificationResult.Failure("$message (timeout = $timeout ms)")
    }
