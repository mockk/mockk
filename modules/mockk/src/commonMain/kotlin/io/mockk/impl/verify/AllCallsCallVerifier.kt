package io.mockk.impl.verify

import io.mockk.Invocation
import io.mockk.MockKGateway
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeToString
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class AllCallsCallVerifier(
    stubRepo: StubRepository,
    safeToString: SafeToString
) : UnorderedCallVerifier(stubRepo, safeToString) {

    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters
    ): MockKGateway.VerificationResult {

        val result = super.verify(verificationSequence, params)
        if (result.matches) {
            val allInvocations = verificationSequence.allInvocations(stubRepo)
            val nonMatchingInvocations = allInvocations
                .filter { invoke -> doesNotMatchAnyCalls(verificationSequence, invoke) }

            if (nonMatchingInvocations.isNotEmpty()) {
                return MockKGateway.VerificationResult.Failure(safeToString.exec {
                    "some calls were not matched: $nonMatchingInvocations" + reportCalls(verificationSequence,
                        allInvocations)
                })
            }

        }
        return result
    }

    private fun doesNotMatchAnyCalls(calls: List<RecordedCall>, invoke: Invocation) =
        !calls.any { call -> call.matcher.match(invoke) }
}