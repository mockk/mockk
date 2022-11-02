package io.mockk.it

import io.mockk.ConstantAnswer
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdditionalAnswerTest {
    val mock = mockk<MockCls>()

    @Test
    fun andReturnsAnswer() {
        every { mock.op(any()) } returns 3 andThen 5

        assertEquals(3, mock.op(1))
        assertEquals(5, mock.op(2))
        assertEquals(5, mock.op(3))
    }

    @Test
    fun andReturnsManyAnswer() {
        every { mock.op(any()) } returns 3 andThenMany listOf(4, 5)

        assertEquals(3, mock.op(1))
        assertEquals(4, mock.op(2))
        assertEquals(5, mock.op(3))
        assertEquals(5, mock.op(4))
    }

    @Test
    fun andReturnsManyTwiceAnswer() {
        every { mock.op(any()) } returns 3 andThenMany listOf(4, 5) andThenMany listOf(6, 7)

        assertEquals(3, mock.op(1))
        assertEquals(4, mock.op(2))
        assertEquals(5, mock.op(3))
        assertEquals(6, mock.op(4))
        assertEquals(7, mock.op(5))
        assertEquals(7, mock.op(6))
    }

    @Test
    fun andReturnsLambdaAnswer() {
        every { mock.op(any()) } returns 3 andThenAnswer { 5 }

        assertEquals(3, mock.op(1))
        assertEquals(5, mock.op(2))
        assertEquals(5, mock.op(3))
    }

    @Test
    fun andReturnsTwoLambdasAnswer() {
        every { mock.op(any()) } returns 3 andThenAnswer  { 4 } andThenAnswer  { 5 }

        assertEquals(3, mock.op(1))
        assertEquals(4, mock.op(2))
        assertEquals(5, mock.op(3))
        assertEquals(5, mock.op(4))
    }

    @Test
    fun andReturnsCoLambdaAnswer() {
        every { mock.op(any()) } returns 3 coAndThen { 5 }

        assertEquals(3, mock.op(1))
        assertEquals(5, mock.op(2))
        assertEquals(5, mock.op(3))
    }

    @Test
    fun andReturnsTwoCoLambdasAnswer() {
        every { mock.op(any()) } returns 3 coAndThen { 4 } coAndThen { 5 }

        assertEquals(3, mock.op(1))
        assertEquals(4, mock.op(2))
        assertEquals(5, mock.op(3))
        assertEquals(5, mock.op(4))
    }

    @Test
    fun andReturnsAnswerClassAnswer() {
        every { mock.op(any()) } returns 3 andThenAnswer ConstantAnswer(5)

        assertEquals(3, mock.op(1))
        assertEquals(5, mock.op(2))
        assertEquals(5, mock.op(3))
    }

    @Test
    fun andReturnsAnswerClassTwiceAnswer() {
        every { mock.op(any()) } returns 3 andThenAnswer ConstantAnswer(4) andThenAnswer ConstantAnswer(5)

        assertEquals(3, mock.op(1))
        assertEquals(4, mock.op(2))
        assertEquals(5, mock.op(3))
        assertEquals(5, mock.op(4))
    }

    @Test
    fun andThrows() {
        every { mock.op(any()) } returns 3 andThenThrows RuntimeException("error")

        assertEquals(3, mock.op(1))
        assertFailsWith(RuntimeException::class) {
            mock.op(2)
        }
    }

    @Test
    fun andThrowsTwice() {
        every { mock.op(any()) } returns 3 andThenThrows IllegalArgumentException("error1") andThenThrows IllegalStateException(
            "error2"
        )

        assertEquals(3, mock.op(1))
        assertFailsWith(IllegalArgumentException::class) {
            mock.op(2)
        }
        assertFailsWith(IllegalStateException::class) {
            mock.op(3)
        }
    }

    @Test
    fun andThrowsMany() {
        val exceptions = listOf(
            IllegalArgumentException("error1"),
            NullPointerException("error2"),
            NoSuchElementException("error3")
        )

        every { mock.op(any()) } throwsMany exceptions

        assertFailsWith(IllegalArgumentException::class) {
            mock.op(2)
        }
        assertFailsWith(NullPointerException::class) {
            mock.op(3)
        }
        assertFailsWith(NoSuchElementException::class) {
            mock.op(3)
        }
        assertFailsWith(NoSuchElementException::class) {
            mock.op(4)
        }
    }

    @Test
    fun andThenThrowsMany() {
        val exceptions = listOf(
            IllegalArgumentException("error1"),
            NullPointerException("error2"),
            NoSuchElementException("error3")
        )
        every { mock.op(any()) } returns 3 andThenThrowsMany exceptions andThen 2

        assertEquals(3, mock.op(1))
        assertFailsWith(IllegalArgumentException::class) {
            mock.op(2)
        }
        assertFailsWith(NullPointerException::class) {
            mock.op(3)
        }
        assertFailsWith(NoSuchElementException::class) {
            mock.op(3)
        }
        assertEquals(2, mock.op(4))
        assertEquals(2, mock.op(5))
    }

    @Test
    fun arbitraryLengthCheck() {
        every {
            mock.op(any())
        }.returns(3)
            .andThen(4)
            .andThenMany(listOf(5, 6))
            .andThen(7)
            .andThenAnswer(ConstantAnswer(8))
            .andThenThrows(IllegalArgumentException())
            .andThenAnswer() { 9 }
            .coAndThen { 10 }
            .andThenThrows(IllegalStateException())
            .andThen(11)

        assertEquals(3, mock.op(1))
        assertEquals(4, mock.op(2))
        assertEquals(5, mock.op(3))
        assertEquals(6, mock.op(4))
        assertEquals(7, mock.op(5))
        assertEquals(8, mock.op(6))
        assertFailsWith(IllegalArgumentException::class) {
            mock.op(7)
        }
        assertEquals(9, mock.op(8))
        assertEquals(10, mock.op(9))
        assertFailsWith(IllegalStateException::class) {
            mock.op(10)
        }
        assertEquals(11, mock.op(11))
        assertEquals(11, mock.op(12))
    }

    class MockCls {
        fun op(a: Int): Int = a + 1
    }
}
