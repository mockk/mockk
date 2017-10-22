package io.mockk

import io.mockk.external.MockKJUnitRunner
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(MockKJUnitRunner::class)
class JUnit4Test {
    class CLS {
        fun abc(a: Int, b: Int): Int {
            return a + b
        }
    }

    @Test
    fun test() {
        val mock = mockk<CLS>()
        every { mock.abc(1, 2) } returns 5
        Assert.assertEquals(5, mock.abc(1, 2))
    }
}
