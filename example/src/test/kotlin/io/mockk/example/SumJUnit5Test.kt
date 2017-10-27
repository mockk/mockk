package io.mockk.example

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.jupiter.api.Test

class SumJUnit5Test {
    class Abc {
        fun sum(a: Int, b: Int) = a + b
    }

    @Test
    fun sum13shouldBe5() {
        val mock = mockk<Abc>()

        every { mock.sum(1, 3) } returns 5

        Assert.assertEquals(5, mock.sum(1, 3))

        verify { mock.sum(1, 3) }
    }
}