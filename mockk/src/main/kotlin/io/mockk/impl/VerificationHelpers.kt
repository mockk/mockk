package io.mockk.impl

import io.mockk.InternalPlatform
import io.mockk.Invocation
import io.mockk.MatchedCall


object VerificationHelpers {
    fun formatCalls(calls: List<Invocation>): String {
        return calls.map {
            it.toString()
        }.joinToString("\n")
    }

    fun List<MatchedCall>.allInvocations(stubRepo: StubRepository) =
            this.map { InternalPlatform.ref(it.invocation.self) }
                    .distinct()
                    .map { it.value }
                    .flatMap { stubRepo.stubFor(it).allRecordedCalls() }
                    .sortedBy { it.timestamp }

    fun reportCalls(calls: List<MatchedCall>, allCalls: List<Invocation>): String {
        return "\nMatchers: \n" + calls.map { it.matcher.toString() }.joinToString("\n") +
                "\nCalls: \n" + formatCalls(allCalls)
    }
}

