package io.mockk.gh

import io.mockk.mockk
import kotlin.test.Test

internal class TestFooIssue119: TestBarIssue119()

class Issue119Test {
    @Test
    fun test() {
        mockk<TestFooIssue119>(relaxed = true)
    }
}
