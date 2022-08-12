package io.mockk.it

import io.mockk.every
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertEquals

class PartialArgumentMatchingTest {
    class ManyArgsOpClass {
        fun op(
            a: Boolean = true, b: Boolean = true,
            c: Byte = 1, d: Byte = 2,
            e: Short = 3, f: Short = 4,
            g: Char = 5.toChar(), h: Char = 6.toChar(),
            i: Int = 7, j: Int = 8,
            k: Long = 9, l: Long = 10,
            m: Float = 10.0f, n: Float = 11.0f,
            o: Double = 12.0, p: Double = 13.0,
            q: String = "14", r: String = "15",
            s: IntWrapper = IntWrapper(16), t: IntWrapper = IntWrapper(17)
        ): Double {

            return a.toInt() * -1 + b.toInt() * -2 + c + d + e + f + g.code.toByte() + h.code.toByte() +
                    i + j + k + l + m + n + o + p + q.toInt() + r.toInt() + s.data + t.data
        }

        private fun Boolean.toInt() = if (this) 1 else 0

        data class IntWrapper(val data: Int)
    }

    val spy = spyk(ManyArgsOpClass())

    @Test
    fun passThrough() {
        assertEquals(160.0, spy.op())
    }


    @Test
    fun allAny() {
        every { spy.op(allAny()) } returns 1.0

        assertEquals(1.0, spy.op())
    }


    @Test
    fun firstBooleanArg() {
        every { spy.op(a = eq(false)) } returns 1.0

        assertEquals(1.0, spy.op(a = false))
    }

    @Test
    fun secondBooleanArg() {
        every { spy.op(b = eq(false)) } returns 1.0

        assertEquals(1.0, spy.op(b = false))
    }


    @Test
    fun firstByteArg() {
        every { spy.op(c = 5.toByte()) } returns 1.0

        assertEquals(1.0, spy.op(c = 5.toByte()))
    }

    @Test
    fun secondByteArg() {
        every { spy.op(d = 5.toByte()) } returns 1.0

        assertEquals(1.0, spy.op(d = 5.toByte()))
    }


    @Test
    fun firstShortArg() {
        every { spy.op(e = 5) } returns 1.0

        assertEquals(1.0, spy.op(e = 5))
    }

    @Test
    fun secondShortArg() {
        every { spy.op(f = 5) } returns 1.0

        assertEquals(1.0, spy.op(f = 5))
    }


    @Test
    fun firstCharArg() {
        every { spy.op(g = 3.toChar()) } returns 1.0

        assertEquals(1.0, spy.op(g = 3.toChar()))
    }

    @Test
    fun secondCharArg() {
        every { spy.op(h = 3.toChar()) } returns 1.0

        assertEquals(1.0, spy.op(h = 3.toChar()))
    }


    @Test
    fun firstIntArg() {
        every { spy.op(i = 5) } returns 1.0

        assertEquals(1.0, spy.op(i = 5))
    }

    @Test
    fun secondIntArg() {
        every { spy.op(j = 5) } returns 1.0

        assertEquals(1.0, spy.op(j = 5))
    }


    @Test
    fun firstLongArg() {
        every { spy.op(k = 5) } returns 1.0

        assertEquals(1.0, spy.op(k = 5))
    }

    @Test
    fun secondLongArg() {
        every { spy.op(k = 5) } returns 1.0

        assertEquals(1.0, spy.op(k = 5))
    }


    @Test
    fun firstFloatArg() {
        every { spy.op(m = 5f) } returns 1.0

        assertEquals(1.0, spy.op(m = 5f))
    }

    @Test
    fun secondFloatArg() {
        every { spy.op(n = 5f) } returns 1.0

        assertEquals(1.0, spy.op(n = 5f))
    }


    @Test
    fun firstDoubleArg() {
        every { spy.op(o = 5.0) } returns 1.0

        assertEquals(1.0, spy.op(o = 5.0))
    }

    @Test
    fun secondDoubleArg() {
        every { spy.op(p = 5.0) } returns 1.0

        assertEquals(1.0, spy.op(p = 5.0))
    }


    @Test
    fun firstStringArg() {
        every { spy.op(q = "5") } returns 1.0

        assertEquals(1.0, spy.op(q = "5"))
    }

    @Test
    fun secondStringArg() {
        every { spy.op(r = "5") } returns 1.0

        assertEquals(1.0, spy.op(r = "5"))
    }


    @Test
    fun firstIntWrapperArg() {
        every { spy.op(s = ManyArgsOpClass.IntWrapper(5)) } returns 1.0

        assertEquals(1.0, spy.op(s = ManyArgsOpClass.IntWrapper(5)))
    }

    @Test
    fun secondIntWrapperArg() {
        every { spy.op(t = ManyArgsOpClass.IntWrapper(5)) } returns 1.0

        assertEquals(1.0, spy.op(t = ManyArgsOpClass.IntWrapper(5)))
    }

}