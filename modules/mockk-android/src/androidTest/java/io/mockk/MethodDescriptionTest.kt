package io.mockk

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.util.concurrent.Callable
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MethodDescriptionTest {

    /**
     * Verifies issue #170.
     */
    @Test
    fun test() {
        val mock = mockk<Callable<String>>()
        every { mock.call() } returns "test"
        assertEquals("test", mock.call())
    }
}
