package io.mockk.impl

import io.mockk.Invocation
import io.mockk.MatchedCall
import io.mockk.MockKGateway
import io.mockk.impl.VerificationHelpers.allInvocations

internal class AllCallVerifierImpl(private val gateway: MockKGatewayImpl) : UnorderedCallVerifierImpl(gateway) {
    override fun verify(calls: List<MatchedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        val result = super.verify(calls, min, max)
        if (result.matches) {
            val nonMatchingInvocations = calls.allInvocations(gateway)
                    .filter { invoke -> doesNotMatchAnyCalls(calls, invoke) }

            if (nonMatchingInvocations.isNotEmpty()) {
                return MockKGateway.VerificationResult(false, "some calls were not matched: $nonMatchingInvocations")
            }

        }
        return result
    }

    private fun doesNotMatchAnyCalls(calls: List<MatchedCall>, invoke: Invocation) =
            !calls.any { call -> call.matcher.match(invoke) }
}