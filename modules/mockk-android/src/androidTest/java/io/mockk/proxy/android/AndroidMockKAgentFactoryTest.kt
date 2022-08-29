package io.mockk.proxy.android

import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMockKAgentFactoryTest {

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
