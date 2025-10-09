package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerifyAtLeastAtMostExactlyTest {
    class MockCls {
        fun op(a: Int) = a + 1
    }

    val mock = mockk<MockCls>()

    @Test
    fun atLeast() {
        doCalls()

        verify(atLeast = 4) {
            mock.op(1)
        }
    }

    @Test
    fun atLeastInverse() {
        doCalls()

        verify(atLeast = 5, inverse = true) {
            mock.op(1)
        }
    }

    @Test
    fun exactly() {
        doCalls()

        verify(exactly = 4) {
            mock.op(1)
        }
    }

    @Test
    fun exactlyInverse() {
        doCalls()

        verify(exactly = 3, inverse = true) {
            mock.op(1)
        }
    }

    @Test
    fun atMost() {
        doCalls()

        verify(atMost = 4) {
            mock.op(1)
        }
    }

    @Test
    fun atMostInverse() {
        doCalls()

        verify(atMost = 3, inverse = true) {
            mock.op(1)
        }
    }

    @Test
    fun exactlyZero() {
        doCalls()

        verify(exactly = 0) {
            mock.op(3)
        }
    }


    @Test
    fun exactlyOnce() {
        doCalls()

        verify(exactly = 1) {
            mock.op(0)
        }
    }

    @Test
    fun exactlyTwiceInverse() {
        doCalls()

        verify(exactly = 2, inverse = true) {
            mock.op(0)
        }
    }

    @Test
    fun exactlyZeroInverse() {
        doCalls()

        verify(exactly = 0, inverse = true) {
            mock.op(0)
        }
    }

    @Test
    fun wasNotCalled() {
        val secondMock = mockk<MockCls>()
        val thirdMock = mockk<MockCls>()

        verify {
            listOf(secondMock, thirdMock) wasNot Called
        }
    }

    @Test
    fun simple() {
        doCalls()

        verify { mock.op(0) }
    }

    @Test
    fun order() {
        doCalls()

        verifyOrder {
            mock.op(1)
            mock.op(1)
            mock.op(1)
            mock.op(1)
        }
    }

    @Test
    fun sequence() {
        doCalls()

        verifySequence {
            mock.op(0)
            mock.op(1)
            mock.op(1)
            mock.op(1)
            mock.op(1)
        }
    }

    fun doCalls() {
        every { mock.op(0) } throws RuntimeException("test")
        every { mock.op(1) } returnsMany listOf(1, 2, 3)

        assertFailsWith(RuntimeException::class) {
            mock.op(0)
        }

        assertEquals(1, mock.op(1))
        assertEquals(2, mock.op(1))
        assertEquals(3, mock.op(1))
        assertEquals(3, mock.op(1))
    }

}