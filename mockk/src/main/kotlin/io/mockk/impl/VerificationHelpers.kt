package io.mockk.impl

import io.mockk.*


object VerificationHelpers {
    fun formatCalls(calls: List<Invocation>): String {
        return calls.map {
            it.toString()
        }.joinToString("\n")
    }

    fun List<MatchedCall>.allInvocations(gateway: MockKGatewayImpl) =
            this.map { InternalPlatform.ref(it.invocation.self) }
                    .distinct()
                    .map { it.value }
                    .flatMap { gateway.stubFor(it).allRecordedCalls() }
                    .sortedBy { it.timestamp }

    fun reportCalls(calls: List<MatchedCall>, allCalls: List<Invocation>): String {
        return "\nMatchers: \n" + calls.map { it.matcher.toString() }.joinToString("\n") +
                "\nCalls: \n" + formatCalls(allCalls)
    }
}

