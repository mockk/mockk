package io.mockk.impl.recording

import io.mockk.Invocation
import io.mockk.MockKGateway
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.SafeToString
import io.mockk.impl.stub.Stub
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers

class CommonVerificationAcknowledger(
    val stubRepo: StubRepository,
    val safeToString: SafeToString
) : MockKGateway.VerificationAcknowledger {

    override fun markCallVerified(invocation: Invocation) {
        (invocation.stub as? Stub)?.markCallVerified(invocation)
    }

    override fun acknowledgeVerified() {
        stubRepo.allStubs.forEach { acknowledgeVerificationHelper(it) }
    }

    override fun acknowledgeVerified(mock: Any) {
        val stub = stubRepo.stubFor(mock)
        acknowledgeVerificationHelper(stub)
    }

    override fun checkUnnecessaryStub() {
        stubRepo.allStubs.forEach { checkUnnecessaryStubHelper(it) }
    }

    override fun checkUnnecessaryStub(mock: Any) {
        val stub = stubRepo.stubFor(mock)
        checkUnnecessaryStubHelper(stub)
    }

    private fun acknowledgeVerificationHelper(stub: Stub) {
        val allCalls = stub.allRecordedCalls().map { InternalPlatform.ref(it) }.toHashSet()
        val verifiedCalls = stub.verifiedCalls().map { InternalPlatform.ref(it) }.toHashSet()

        if (allCalls == verifiedCalls) return

        val nonVerifiedReport =
            safeToString.exec {
                reportNotVerified(
                    allCalls.size,
                    verifiedCalls.size,
                    stub.allRecordedCalls() - stub.verifiedCalls().toSet()
                )
            }
        throw AssertionError("Verification acknowledgment failed$nonVerifiedReport")
    }


    private fun reportNotVerified(
        nTotal: Int,
        nVerified: Int,
        notVerified: List<Invocation>
    ): String {
        return "\n\nVerified call count: $nVerified\n" +
                "Recorded call count: $nTotal\n" +
                "\n\nNot verified calls:\n" +
                VerificationHelpers.formatCalls(notVerified) +
                "\n\nStack traces:\n" +
                VerificationHelpers.stackTraces(notVerified)
    }

    private fun checkUnnecessaryStubHelper(stub: Stub) {
        val unnecessaryMatcher = stub.matcherUsages().filterValues { it == 0 }.keys

        if (unnecessaryMatcher.isEmpty()) return

        val report =
            "Unnecessary stubbings detected.\nFollowing stubbings are not used, either because there are unnecessary or because tested code doesn't call them :\n\n" +
                    unnecessaryMatcher
                        .mapIndexed { idx, matcher -> "${idx + 1}) $matcher" }
                        .joinToString("\n")
        throw AssertionError(report)
    }

}
