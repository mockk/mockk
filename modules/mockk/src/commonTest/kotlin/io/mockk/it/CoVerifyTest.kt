package io.mockk.it

import io.mockk.InternalPlatformDsl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyCount
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.coroutineScope

class CoVerifyTest {
    class MockCls {
        suspend fun op(a: Int) = coroutineScope { a + 1 }
    }

    val mock = mockk<MockCls>()

    private fun doCalls() {
        coEvery { mock.op(5) } returns 1
        coEvery { mock.op(6) } returns 2
        coEvery { mock.op(7) } returns 3

        InternalPlatformDsl.runCoroutine {
            assertEquals(1, mock.op(5))
            assertEquals(2, mock.op(6))
            assertEquals(3, mock.op(7))
        }
    }

    @Test
    fun checkVerify() {
        doCalls()

        coVerify {
            mock.op(6)
            mock.op(5)
        }
    }

    @Test
    fun checkVerifyInverse1() {
        doCalls()

        coVerify(inverse = true) {
            mock.op(6)
            mock.op(8)
        }
    }

    @Test
    fun checkVerifyInverse2() {
        doCalls()

        coVerify(inverse = true) {
            mock.op(4)
            mock.op(8)
        }
    }

    @Test
    fun checkVerifyOrder1() {
        doCalls()

        coVerifyOrder {
            mock.op(5)
            mock.op(7)
        }
    }

    @Test
    fun checkVerifyOrder2() {
        doCalls()

        coVerifyOrder {
            mock.op(5)
            mock.op(6)
        }
    }

    @Test
    fun checkVerifyOrder3() {
        doCalls()

        coVerifyOrder {
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun checkVerifyOrderInverse1() {
        doCalls()

        coVerifyOrder(inverse = true) {
            mock.op(7)
            mock.op(5)
        }
    }

    @Test
    fun checkVerifyOrderInverse2() {
        doCalls()

        coVerifyOrder(inverse = true) {
            mock.op(5)
            mock.op(4)
        }
    }

    @Test
    fun checkVerifyOrderInverse3() {
        doCalls()

        coVerifyOrder(inverse = true) {
            mock.op(4)
            mock.op(8)
        }
    }

    @Test
    fun verifySequence() {
        doCalls()

        coVerifySequence {
            mock.op(5)
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun verifySequenceInverse1() {
        doCalls()

        coVerifySequence(inverse = true) {
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun verifySequenceInverse2() {
        doCalls()

        coVerifySequence(inverse = true) {
            mock.op(7)
            mock.op(6)
            mock.op(5)
        }
    }

    @Test
    fun verifySequenceInverse3() {
        doCalls()

        coVerifySequence(inverse = true) {
            mock.op(6)
            mock.op(5)
            mock.op(7)
        }
    }

    @Test
    fun verifyCount() {
        doCalls()

        coVerifyCount {
            0 * { mock.op(4) } // not called
            1 * { mock.op(5) } // called
            (0..Int.MAX_VALUE) * { mock.op(6) } // called
            (1..1) * { mock.op(7) } // called
            (0..0) * { mock.op(8) } // not called
            (0..1) * { mock.op(9) } // not called
        }
    }
}
