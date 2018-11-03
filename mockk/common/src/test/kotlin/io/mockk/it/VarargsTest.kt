package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class VarargsTest {
    val mock = mockk<VarargsCls>()

    @Test
    fun simpleArgs() {
        every { mock.varArgsOp(5, 6, 7, c = 8) } returns 1
        assertEquals(1, mock.varArgsOp(5, 6, 7, c = 8))
        verify { mock.varArgsOp(5, 6, more(5), c = 8) }
    }

    @Test
    fun eqMatcher() {
        every { mock.varArgsOp(6, eq(3), 7, c = 8) } returns 2
        assertEquals(2, mock.varArgsOp(6, 3, 7, c = 8))
        verify { mock.varArgsOp(6, any(), more(5), c = 8) }
    }

    @Test
    fun eqAnyMatchers() {
        every { mock.varArgsOp(7, eq(3), any(), c = 8) } returns 3
        assertEquals(3, mock.varArgsOp(7, 3, 22, c = 8))
        val slot = slot<Int>()
        verify { mock.varArgsOp(7, capture(slot), more(20), c = 8) }
        assertEquals(3, slot.captured)
    }

    class VarargsCls {
        fun varArgsOp(a: Int, vararg b: Int, c: Int, d: Int = 6) = b.sum() + a
    }
}

