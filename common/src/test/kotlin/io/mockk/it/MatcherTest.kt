package io.mockk.it

import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MatcherTest {
    interface Wrapper
    data class IntWrapper(val data: Int) : Wrapper

    class MockCls {
        fun op(a: Int, b: Int): Int = a + b

        fun op(a: Wrapper?, b: Wrapper?): Int {
            return if (a is IntWrapper && b is IntWrapper) {
                a.data + b.data
            } else {
                0
            }
        }
    }

    @MockK
    lateinit var mock: MockCls

    @BeforeTest
    fun init() {
        MockKAnnotations.init(this)
    }

    @Test
    fun eqRefEq() {
        val a = IntWrapper(3)
        val b = IntWrapper(4)

        every { mock.op(eq(a), refEq(b)) } returns 1

        assertEquals(1, mock.op(a, b))
        assertFailsWith<MockKException> { mock.op(a, IntWrapper(4)) }
        assertEquals(1, mock.op(IntWrapper(3), b))

        verify {
            mock.op(IntWrapper(3), b)
        }
    }

    @Test
    fun less() {
        every { mock.op(1, less(2)) } returns 1
        every { mock.op(2, less(2, andEquals = true)) } returns 2

        assertEquals(1, mock.op(1, 0))
        assertEquals(1, mock.op(1, 1))
        assertFailsWith<MockKException> { mock.op(1, 2) }
        assertFailsWith<MockKException> { mock.op(1, 3) }

        assertEquals(2, mock.op(2, 0))
        assertEquals(2, mock.op(2, 1))
        assertEquals(2, mock.op(2, 2))
        assertFailsWith<MockKException> { mock.op(2, 3) }

        verify {
            mock.op(1, 0)
            mock.op(1, 1)
            mock.op(2, 0)
            mock.op(2, 1)
            mock.op(2, 2)
        }
    }

    @Test
    fun range() {
        every { mock.op(1, range(2, 4)) } returns 1
        every { mock.op(2, range(2, 4, fromInclusive = false)) } returns 2
        every { mock.op(3, range(2, 4, toInclusive = false)) } returns 3

        assertFailsWith<MockKException> { mock.op(1, 0) }
        assertFailsWith<MockKException> { mock.op(1, 1) }
        assertEquals(1, mock.op(1, 2))
        assertEquals(1, mock.op(1, 3))
        assertEquals(1, mock.op(1, 4))
        assertFailsWith<MockKException> { mock.op(1, 5) }
        assertFailsWith<MockKException> { mock.op(1, 6) }

        assertFailsWith<MockKException> { mock.op(2, 0) }
        assertFailsWith<MockKException> { mock.op(2, 1) }
        assertFailsWith<MockKException> { mock.op(2, 2) }
        assertEquals(2, mock.op(2, 3))
        assertEquals(2, mock.op(2, 4))
        assertFailsWith<MockKException> { mock.op(2, 5) }
        assertFailsWith<MockKException> { mock.op(2, 6) }

        assertFailsWith<MockKException> { mock.op(3, 0) }
        assertFailsWith<MockKException> { mock.op(3, 1) }
        assertEquals(3, mock.op(3, 2))
        assertEquals(3, mock.op(3, 3))
        assertFailsWith<MockKException> { mock.op(3, 4) }
        assertFailsWith<MockKException> { mock.op(3, 5) }
        assertFailsWith<MockKException> { mock.op(3, 6) }

        verify {
            mock.op(1, 2)
            mock.op(1, 3)
            mock.op(1, 4)

            mock.op(1, 3)
            mock.op(1, 4)

            mock.op(1, 2)
            mock.op(1, 3)
        }
    }

    @Test
    fun cmpEq() {
        every { mock.op(1, cmpEq(2)) } returns 1

        assertFailsWith<MockKException> { mock.op(1, 0) }
        assertFailsWith<MockKException> { mock.op(1, 1) }
        assertEquals(1, mock.op(1, 2))
        assertFailsWith<MockKException> { mock.op(1, 3) }
        assertFailsWith<MockKException> { mock.op(1, 4) }

        verify {
            mock.op(1, 2)
        }
    }

    @Test
    fun more() {
        every { mock.op(1, more(2)) } returns 1
        every { mock.op(2, more(2, andEquals = true)) } returns 2

        assertEquals(1, mock.op(1, 4))
        assertEquals(1, mock.op(1, 3))
        assertFailsWith<MockKException> { mock.op(1, 2) }
        assertFailsWith<MockKException> { mock.op(1, 1) }

        assertEquals(2, mock.op(2, 4))
        assertEquals(2, mock.op(2, 3))
        assertEquals(2, mock.op(2, 2))
        assertFailsWith<MockKException> { mock.op(2, 1) }

        verify {
            mock.op(1, 4)
            mock.op(1, 3)
            mock.op(2, 4)
            mock.op(2, 3)
            mock.op(2, 2)
        }
    }


    @Test
    fun or() {
        every { mock.op(1, or(eq(3), eq(5))) } returns 1

        assertFailsWith<MockKException> { mock.op(1, 2) }
        assertEquals(1, mock.op(1, 3))
        assertFailsWith<MockKException> { mock.op(1, 4) }
        assertEquals(1, mock.op(1, 5))
        assertFailsWith<MockKException> { mock.op(1, 6) }

        verify {
            mock.op(1, 3)
            mock.op(1, 5)
        }
    }

    @Test
    fun and() {
        every { mock.op(1, and(more(8), less(15))) } returns 1

        assertFailsWith<MockKException> { mock.op(1, 7) }
        assertFailsWith<MockKException> { mock.op(1, 8) }
        assertEquals(1, mock.op(1, 9))
        assertEquals(1, mock.op(1, 14))
        assertFailsWith<MockKException> { mock.op(1, 15) }
        assertFailsWith<MockKException> { mock.op(1, 16) }

        verify {
            mock.op(1, 9)
            mock.op(1, 14)
        }
    }

    @Test
    fun not() {
        every { mock.op(1, not(13)) } returns 1

        assertEquals(1, mock.op(1, 12))
        assertFailsWith<MockKException> { mock.op(1, 13) }
        assertEquals(1, mock.op(1, 14))

        verify {
            mock.op(1, 12)
            mock.op(1, 14)
        }
    }

    @Test
    fun compositeExpr1() {
        every { mock.op(1, or(more(20, andEquals = true), 17)) } returns 1

        assertFailsWith<MockKException> { mock.op(1, 16) }
        assertEquals(1, mock.op(1, 17))
        assertFailsWith<MockKException> { mock.op(1, 18) }
        assertFailsWith<MockKException> { mock.op(1, 19) }
        assertEquals(1, mock.op(1, 20))
        assertEquals(1, mock.op(1, 21))

        verify {
            mock.op(1, 17)
            mock.op(1, 20)
            mock.op(1, 21)
        }
    }


    @Test
    fun compositeExpr2() {
        every { mock.op(1, or(or(more(20), 17), 13)) } returns 1

        assertFailsWith<MockKException> { mock.op(1, 12) }
        assertEquals(1, mock.op(1, 13))
        assertFailsWith<MockKException> { mock.op(1, 14) }
        assertFailsWith<MockKException> { mock.op(1, 16) }
        assertEquals(1, mock.op(1, 17))
        assertFailsWith<MockKException> { mock.op(1, 18) }
        assertFailsWith<MockKException> { mock.op(1, 19) }
        assertFailsWith<MockKException> { mock.op(1, 20) }
        assertEquals(1, mock.op(1, 21))

        verify {
            mock.op(1, 13)
            mock.op(1, 17)
            mock.op(1, 20)
            mock.op(1, 21)
        }
    }

    @Test
    fun capture() {
        val v = slot<Int>()
        every { mock.op(1, and(capture(v), more(20))) } answers { v.captured }

        assertFailsWith<MockKException> { mock.op(1, 19) }
        assertFailsWith<MockKException> { mock.op(1, 20) }
        assertEquals(21, mock.op(1, 21))
        assertEquals(22, mock.op(1, 22))

        verify {
            mock.op(1, 21)
            mock.op(1, 22)
        }
    }

    @Test
    fun isNull() {
        every { mock.op(IntWrapper(7), isNull()) } returns 1

        assertEquals(1, mock.op(IntWrapper(7), null))
        assertFailsWith<MockKException> { mock.op(IntWrapper(7), IntWrapper(8)) }

        verify {
            mock.op(IntWrapper(7), null)
        }
    }

    @Test
    fun isNotNull() {
        every { mock.op(IntWrapper(7), isNull(inverse = true)) } returns 1

        assertFailsWith<MockKException> { mock.op(IntWrapper(7), null) }
        assertEquals(1, mock.op(IntWrapper(7), IntWrapper(8)))

        verify {
            mock.op(IntWrapper(7), IntWrapper(8))
        }
    }

    @Test
    fun ofType() {
        every { mock.op(IntWrapper(7), ofType(IntWrapper::class)) } returns 1

        assertFailsWith<MockKException> { mock.op(IntWrapper(7), object : Wrapper {}) }
        assertFailsWith<MockKException> { mock.op(IntWrapper(7), null) }
        assertEquals(1, mock.op(IntWrapper(7), IntWrapper(8)))

        verify {
            mock.op(IntWrapper(7), IntWrapper(8))
        }
    }
}