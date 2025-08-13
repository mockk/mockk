package io.mockk.it

import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyCount
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerifyAtLeastAtMostExactlyTest {

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

    /**
     * See issue #25
     */
    @Test
    fun exactlyZeroWithAny() {
        doCalls2()

        verify(exactly = 0) {
            mock.op2(3, any())
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

    /**
     * See issue #507
     *
     * A regression occurred in version 1.10.2 causing verify order to use
     * eq() instead of any() matcher.
     * This test exist to avoid this kind of regression in the future.
     */
    @Test
    fun orderWithAny() {
        val tracker = mockk<Tracker>(relaxUnitFun = true)
        val player = Player(tracker)

        player.goCrazy()

        verifyOrder {
            tracker.track(any(), "play", "param", any())
            tracker.track(any(), "pause", "param", any())
            tracker.track(any(), "play", "param", any())
            tracker.track(any(), "pause", "param", any())
            tracker.track(any(), "play", "param", any())
        }
    }

    /**
     * See issue #614
     */
    @Test
    fun verifyOrderThrowAssertionErrorIfNoCallHasBeenMade() {
        val mock: MockCls = mockk(relaxed = true, relaxUnitFun = true)

        assertFailsWith<AssertionError> { verifyOrder { mock.op(any()) } }
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

    @Test
    fun count() {
        doCalls()

        verifyCount {
            1 * { mock.op(0) }
            4 * { mock.op(1) }
            0 * { mock.op(2) }
        }
    }

    @Test
    fun atLeastNeverAtAll() {
        every { mock.op(0) } returns 1

        verify(atLeast = 0) { mock.op(0) }
    }

    @Test
    fun atLeastNever() {
        every { mock.op(0) } returns 1
        mock.op(0)

        verify(atLeast = 0) { mock.op(2) }
    }

    @Test
    fun atLeastNeverAtMostOnce() {
        listOf(
            { every { mock.op(any()) } returns 0 },
            { every { mock.op(any()) } returns 1 }
        ).forEach { condition ->
            clearAllMocks()
            every { mock.op2(any(), any()) } returns 5
            condition()

            doCalls3()

            verify(atLeast = 0, atMost = 1) { mock.op(1) }
            verify(exactly = 1) { mock.op2(3, 4) }
        }
    }

    private fun doCalls() {
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

    private fun doCalls2() {
        every { mock.op(0) } throws RuntimeException("test")
        every { mock.op(1) } returnsMany listOf(1, 2, 3) andThen 5
        every { mock.op2(2, 1) } returns 3

        assertFailsWith(RuntimeException::class) {
            mock.op(0)
        }

        assertEquals(1, mock.op(1))
        assertEquals(2, mock.op(1))
        assertEquals(3, mock.op(1))
        assertEquals(5, mock.op(1))
        assertEquals(3, mock.op2(2, 1))
    }

    private fun doCalls3() {
        if (doCalls4()) {
            mock.op2(3, 4)
        }
    }

    private fun doCalls4(): Boolean {
        return mock.op(0) == 0 || mock.op(1) == 1
    }

    class MockCls {
        fun op(a: Int) = a + 1
        fun op2(a: Int, b: Int) = a + b
    }

    interface Tracker {
        fun track(song: String, action: String, param: String, moreParam: Map<String, String>)
    }

    class Player(private val tracker: Tracker) {
        fun goCrazy() {
            tracker.track("song 2", "play", "param", mapOf(Pair("key", "value")))
            tracker.track("song 2", "pause", "param", mapOf(Pair("key", "value")))
            tracker.track("song 2", "play", "param", mapOf(Pair("key", "value")))
            tracker.track("song 2", "pause", "param", mapOf(Pair("key", "value")))
            tracker.track("song 2", "play", "param", mapOf(Pair("key", "value")))
        }
    }
}
