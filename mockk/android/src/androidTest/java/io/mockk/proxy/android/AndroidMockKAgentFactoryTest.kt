package io.mockk.proxy.android

import android.support.test.rule.ActivityTestRule
import android.widget.FrameLayout
import io.mockk.debug.TestActivity
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class AndroidMockKAgentFactoryTest {

    @Rule
    @JvmField
    val rule = ActivityTestRule(TestActivity::class.java)

    /**
     * This tests that the hidden api logic in [AndroidMockKAgentFactory] works. Otherwise, we would fail when mocking
     * the FrameLayout.
     * Verifies issue #563.
     */
    @Test
    fun test() {
        mockk<FrameLayout>()
    }
}
