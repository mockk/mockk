package io.mockk.it

import io.mockk.InternalPlatformDsl
import io.mockk.coEvery
import io.mockk.coVerifyAny
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAny
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VerifyAnyTest {
    class MockCls {
        fun resume() = Unit

        fun toggleResume() = Unit

        fun play(a: Int) = a + 1

        suspend fun coOp(a: Int) = a + 1
    }

    val mock = mockk<MockCls>()

    private fun doCalls(vararg ops: Int) {
        every { mock.resume() } answers { Unit }
        every { mock.toggleResume() } answers { Unit }
        every { mock.play(any()) } answers { firstArg<Int>() + 1 }
        coEvery { mock.coOp(any()) } answers { firstArg<Int>() + 1 }

        for (op in ops) {
            when (op) {
                0 -> mock.resume()
                1 -> mock.toggleResume()
                else -> mock.play(op)
            }
        }
    }

    @Test
    fun oneOfThePatternsCalled() {
        doCalls(0)

        verifyAny {
            mock.resume()
            mock.toggleResume()
        }
    }

    @Test
    fun secondOfThePatternsCalled() {
        doCalls(1)

        verifyAny {
            mock.resume()
            mock.toggleResume()
        }
    }

    @Test
    fun exactlyOneMatchingCall() {
        doCalls(0)

        verifyAny(exactly = 1) {
            mock.resume()
            mock.toggleResume()
        }
    }

    @Test
    fun exactlyOneFailsWhenBothCalled() {
        doCalls(0, 1)

        val exception =
            assertFailsWith<AssertionError> {
                verifyAny(exactly = 1) {
                    mock.resume()
                    mock.toggleResume()
                }
            }
        assertTrue(exception.message!!.contains("needs exactly 1 calls"))
    }

    @Test
    fun exactlyOneFailsWhenNoneCalled() {
        val emptyMock = mockk<MockCls>()

        val exception =
            assertFailsWith<AssertionError> {
                verifyAny(exactly = 1) {
                    emptyMock.resume()
                    emptyMock.toggleResume()
                }
            }
        assertTrue(exception.message!!.contains("0 matching calls found"))
    }

    @Test
    fun exactlyOneFailsWhenSamePatternCalledTwice() {
        doCalls(0, 0)

        val exception =
            assertFailsWith<AssertionError> {
                verifyAny(exactly = 1) {
                    mock.resume()
                    mock.toggleResume()
                }
            }
        assertTrue(exception.message!!.contains("needs exactly 1 calls"))
    }

    @Test
    fun overlappingPatternsCountedOnce() {
        doCalls(5)

        verifyAny(exactly = 1) {
            mock.play(5)
            mock.play(any())
        }
    }

    @Test
    fun atLeastAtMostBounds() {
        doCalls(5, 6)

        verifyAny(atLeast = 2) {
            mock.play(5)
            mock.play(6)
        }

        verifyAny(atMost = 2) {
            mock.play(5)
            mock.play(6)
        }

        assertFailsWith<AssertionError> {
            verifyAny(atLeast = 3) {
                mock.play(5)
                mock.play(6)
            }
        }
    }

    @Test
    fun inverseVerification() {
        doCalls(0)

        verifyAny(inverse = true) {
            mock.toggleResume()
        }

        assertFailsWith<AssertionError> {
            verifyAny(inverse = true) {
                mock.resume()
            }
        }
    }

    @Test
    fun coVerifyAnySimple() {
        coEvery { mock.coOp(any()) } answers { firstArg<Int>() + 1 }

        InternalPlatformDsl.runCoroutine {
            mock.coOp(5)
        }

        coVerifyAny {
            mock.coOp(5)
            mock.coOp(6)
        }
    }

    @Test
    fun coVerifyAnyExactlyOne() {
        coEvery { mock.coOp(any()) } answers { firstArg<Int>() + 1 }

        InternalPlatformDsl.runCoroutine {
            mock.coOp(5)
        }

        coVerifyAny(exactly = 1) {
            mock.coOp(5)
            mock.coOp(6)
        }
    }
}
