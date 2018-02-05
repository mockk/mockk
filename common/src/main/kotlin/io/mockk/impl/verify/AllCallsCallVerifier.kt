package io.mockk.impl.verify

import io.mockk.Invocation
import io.mockk.MockKGateway
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeLog
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.allInvocations
import io.mockk.impl.verify.VerificationHelpers.reportCalls

class AllCallsCallVerifier(
    stubRepo: StubRepository,
    safeLog: SafeLog
) : UnorderedCallVerifier(stubRepo, safeLog) {
    override fun verify(verificationSequence: List<RecordedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val result = super.verify(verificationSequence, min, max)
        if (result.matches) {
            val allInvocations = verificationSequence.allInvocations(stubRepo)
            val nonMatchingInvocations = allInvocations
                .filter { invoke -> doesNotMatchAnyCalls(verificationSequence, invoke) }

            if (nonMatchingInvocations.isNotEmpty()) {
                return MockKGateway.VerificationResult(false, safeLog.exec {
                    "some calls were not matched: $nonMatchingInvocations" + reportCalls(verificationSequence, allInvocations)
                })
            }

        }
        return result
    }

    private fun doesNotMatchAnyCalls(calls: List<RecordedCall>, invoke: Invocation) =
        !calls.any { call -> call.matcher.match(invoke) }
}