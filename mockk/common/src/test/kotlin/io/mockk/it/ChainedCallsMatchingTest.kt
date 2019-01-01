package io.mockk.it

import io.mockk.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNUSED_PARAMETER")
class ChainedCallsMatchingTest {
    class ChainedOpClass {
        fun chainOp(a: Int, b: Int) = this

        fun otherOp(a: Int, b: Int) = a + b
    }

    val mock = mockk<ChainedOpClass>()

    @BeforeTest
    fun setUp() {
        every { mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4) } returns 1
        every { mock.chainOp(1, 2).chainOp(3, 7).otherOp(2, 5) } returns 2
        every { mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8) } returns 3
        every { mock.chainOp(5, 6).chainOp(7, 8).otherOp(9, 6) } returns 4
        every { mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12) } returns 5
    }

    fun doCalls() {
        assertEquals(1, mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4))
        assertEquals(2, mock.chainOp(1, 2).chainOp(3, 7).otherOp(2, 5))
        assertEquals(3, mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8))
        assertEquals(4, mock.chainOp(5, 6).chainOp(7, 8).otherOp(9, 6))
        assertEquals(5, mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12))
    }

    @Test
    fun checkUnordered() {
        doCalls()

        verify {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
        }
    }

    @Test
    fun checkOrdered1() {
        doCalls()

        verifyOrder {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
        }
    }

    @Test
    fun checkOrdered2() {
        doCalls()

        verifyOrder {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
        }
    }

    @Test
    fun checkOrdered3() {
        doCalls()

        verifyOrder {
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
        }
    }

    @Test
    fun checkSequence() {
        doCalls()

        verifySequence {
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(1, 2).chainOp(3, 7).otherOp(2, 5)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(9, 6)
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
        }
    }

    @Test
    fun checkAll() {
        doCalls()

        verifyAll {
            mock.chainOp(9, 10).chainOp(9, 10).otherOp(11, 12)
            mock.chainOp(1, 2).chainOp(5, 6).otherOp(3, 4)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(9, 6)
            mock.chainOp(5, 6).chainOp(7, 8).otherOp(7, 8)
            mock.chainOp(1, 2).chainOp(3, 7).otherOp(2, 5)
        }
    }
}

