package io.mockk

import java.util.concurrent.Callable
import kotlin.test.Test
import kotlin.test.assertEquals

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