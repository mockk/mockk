package io.mockk.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_DATA_CLASS
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_ENUM
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_INTERFACE
import io.mockk.lint.MockkUnnecessaryUsageDetector.Companion.ISSUE_PRIMITIVE

@Suppress("unused")
internal class LintRegistry : IssueRegistry() {

    override val vendor = Vendor(
        identifier = "MockK",
        vendorName = "MockK",
        feedbackUrl = "https://github.com/mockk/mockk/issues",
        contact = "https://github.com/mockk/mockk"
    )

    override val issues: List<Issue> = listOf(
        ISSUE_DATA_CLASS,
        ISSUE_ENUM,
        ISSUE_INTERFACE,
        ISSUE_PRIMITIVE,
    )

    override val api: Int = CURRENT_API

}
