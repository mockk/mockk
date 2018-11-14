package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.Callable
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue170Test {
    @Test
    fun test() {
        val mock = mockk<Callable<String>>()
        every { mock.call() } returns "test"
        assertEquals("test", mock.call())
    }
}