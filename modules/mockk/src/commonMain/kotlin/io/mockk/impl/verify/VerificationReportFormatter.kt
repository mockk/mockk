package io.mockk.impl.verify

import io.mockk.Invocation

internal object VerificationReportFormatter {
    fun reportNotVerified(
        nTotal: Int,
        nVerified: Int,
        notVerified: List<Invocation>,
    ): String =
        "\n\nVerified call count: $nVerified\n" +
            "Recorded call count: $nTotal\n" +
            "\n\nNot verified calls:\n" +
            VerificationHelpers.formatCalls(notVerified) +
            "\n\nStack traces:\n" +
            VerificationHelpers.stackTraces(notVerified)
}
