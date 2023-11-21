package io.mockk.impl.verify

import io.mockk.MockKGateway.*
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.StubRepository

class TimeoutVerifier(
    val stubRepo: StubRepository,
    val verifierChain: CallVerifier
) : CallVerifier {

    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters
    ): VerificationResult {
        val stubs = verificationSequence.allStubs(stubRepo)

        val session = stubRepo.openRecordCallAwaitSession(stubs, params.timeout)
        val startTime = System.currentTimeMillis()
        try {
            while (true) {
                val result = verifierChain.verify(verificationSequence, params)
                // With atMost set the reaching expected result does not mean test passed
                //  - it can fail till the end of the timeout
                if (params.inverse != result.matches && 
                        (params.max != Int.MAX_VALUE && (System.currentTimeMillis() - startTime < params.timeout))) {
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

    override fun captureArguments() {
        verifierChain.captureArguments()
    }

    private fun List<RecordedCall>.allStubs(stubRepo: StubRepository) =
        this.map { InternalPlatform.ref(it.matcher.self) }
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
