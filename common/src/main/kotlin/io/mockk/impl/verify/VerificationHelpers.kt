package io.mockk.impl.verify

import io.mockk.Invocation
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.StubRepository

object VerificationHelpers {
    fun formatCalls(calls: List<Invocation>): String {
        return calls.map {
            it.toString()
        }.joinToString("\n")
    }

    fun List<RecordedCall>.allInvocations(stubRepo: StubRepository) =
        this.map { InternalPlatform.ref(it.matcher.self) }
            .distinct()
            .map { it.value }
            .flatMap { stubRepo.stubFor(it).allRecordedCalls() }
            .sortedBy { it.timestamp }

    fun reportCalls(calls: List<RecordedCall>, allCalls: List<Invocation>): String {
        return "\nMatchers: \n" + calls.map { it.matcher.toString() }.joinToString("\n") +
                "\nCalls: \n" + formatCalls(allCalls)
    }
}

