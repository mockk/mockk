package io.mockk.example

import io.mockk.every
import io.mockk.junit.MockKJUnit4Runner
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

class SumJUnit4Test {
    class Abc {
        fun sum(a: Int, b: Int) = a + b
    }

    @Test
    fun sum13shouldBe5() {
        val mock = mockk<Abc>()

        every { mock.sum(1, 3) } returns 5

        assertEquals(5, mock.sum(1, 3))

        verify { mock.sum(1, 3) }
    }
}

