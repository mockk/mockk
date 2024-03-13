package io.mockk.it

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyCount
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertEquals

class VerifyTest {

    val mock = mockk<MockCls>()

    private fun doCalls() {
        every { mock.op(5) } returns 1
        every { mock.op(6) } returns 2
        every { mock.op(7) } returns 3

        assertEquals(1, mock.op(5))
        assertEquals(2, mock.op(6))
        assertEquals(3, mock.op(7))
    }

    @Test
    fun checkVerify() {
        doCalls()

        verify {
            mock.op(6)
            mock.op(5)
        }
    }

    @Test
    fun checkVerifyInverse1() {
        doCalls()

        verify(inverse = true) {
            mock.op(6)
            mock.op(8)
        }
    }

    @Test
    fun checkVerifyInverse2() {
        doCalls()

        verify(inverse = true) {
            mock.op(4)
            mock.op(8)
        }
    }

    @Test
    fun checkVerifyOrder1() {
        doCalls()

        verifyOrder {
            mock.op(5)
            mock.op(7)
        }
    }

    @Test
    fun checkVerifyOrder2() {
        doCalls()

        verifyOrder {
            mock.op(5)
            mock.op(6)
        }
    }

    @Test
    fun checkVerifyOrder3() {
        doCalls()

        verifyOrder {
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun checkVerifyOrderInverse1() {
        doCalls()

        verifyOrder(inverse = true) {
            mock.op(7)
            mock.op(5)
        }
    }

    @Test
    fun checkVerifyOrderInverse2() {
        doCalls()

        verifyOrder(inverse = true) {
            mock.op(5)
            mock.op(4)
        }
    }

    @Test
    fun checkVerifyOrderInverse3() {
        doCalls()

        verifyOrder(inverse = true) {
            mock.op(4)
            mock.op(8)
        }
    }

    @Test
    fun verifySequence() {
        doCalls()

        verifySequence {
            mock.op(5)
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun verifySequenceInverse1() {
        doCalls()

        verifySequence(inverse = true) {
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun verifySequenceInverse2() {
        doCalls()

        verifySequence(inverse = true) {
            mock.op(7)
            mock.op(6)
            mock.op(5)
        }
    }

    @Test
    fun verifySequenceInverse3() {
        doCalls()

        verifySequence(inverse = true) {
            mock.op(6)
            mock.op(5)
            mock.op(7)
        }
    }

    @Test
    fun verifyCount() {
        doCalls()

        verifyCount {
            0 * { mock.op(4) } // not called
            1 * { mock.op(5) } // called
            (0..Int.MAX_VALUE) * { mock.op(6) } // called
            (1..1) * { mock.op(7) } // called
            (0..0) * { mock.op(8) } // not called
            (0..1) * { mock.op(9) } // not called
        }
    }

    /**
     * See issue #109
     */
    @Test
    fun verifyWithToString() {
        val foo = mockk<Foo>()
        val bar = mockk<Bar>()

        every { bar.baz("$foo") } just runs

        bar.baz("$foo")

        verify(exactly = 1) { bar.baz("$foo") }

    }

    class Bar {
        fun baz(foo: String) {
            println(foo)
        }
    }

    class Foo {
        override fun toString(): String {
            return "foo"
        }
    }

    class MockCls {
        fun op(a: Int) = a + 1
    }
}
