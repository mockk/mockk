package io.mockk.gh

import android.support.test.rule.ActivityTestRule
import android.widget.FrameLayout
import io.mockk.debug.TestActivity
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class Issue563Test {

    @Rule
    @JvmField
    val rule = ActivityTestRule(TestActivity::class.java)

    @Test
    fun test() {
        // This tests that the hidden api logic in AndroidMockKAgentFactory works. Otherwise,
        // we would fail when mocking the FrameLayout here
        mockk<FrameLayout>()
    }
}
