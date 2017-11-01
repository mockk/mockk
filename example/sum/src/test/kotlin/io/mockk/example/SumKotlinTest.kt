package io.mockk.example

import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.junit.MockKJUnit4Runner
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.runner.RunWith

@RunWith(MockKJUnit4Runner::class)
class SumKotlinTest : StringSpec({
    class Abc {
        fun sum(a: Int, b: Int) = a + b
    }

    "mocked sum 1+3 should be 5" {
        val mock = mockk<Abc>()

        every { mock.sum(1, 3) } returns 5

        Assert.assertEquals(5, mock.sum(1, 3))

        verify { mock.sum(1, 3) }
    }
})