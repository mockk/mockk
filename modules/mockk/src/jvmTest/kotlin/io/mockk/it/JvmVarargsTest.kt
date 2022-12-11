package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmVarargsTest {
    val mock = mockk<JvmVarArgsCls>()

    @Test
    fun simpleArgs() {
        every { mock.varArgsOp(5, 6, 7) } returns 1
        assertEquals(1, mock.varArgsOp(5, 6, 7))
        verify { mock.varArgsOp(5, 6, more(5)) }
    }

    @Test
    fun eqMatcher() {
        every { mock.varArgsOp(6, eq(3), 7) } returns 2
        assertEquals(2, mock.varArgsOp(6, 3, 7))
        verify { mock.varArgsOp(6, any(), more(5)) }
    }

    @Test
    fun eqAnyMatchers() {
        every { mock.varArgsOp(7, eq(4), any()) } returns 3
        assertEquals(3, mock.varArgsOp(7, 4, 22))
        val slot = slot<Int>()
        verify { mock.varArgsOp(7, capture(slot), more(20)) }
        assertEquals(4, slot.captured)
    }
}