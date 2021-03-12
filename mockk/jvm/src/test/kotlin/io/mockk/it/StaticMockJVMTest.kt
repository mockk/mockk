package io.mockk.it

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

val Int.selfOp get() = this * this

class StaticMockJVMTest {

    @Test
    fun extensionFunctionStaticMock() {
        mockkStatic(Int::op)

        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify { 5 op 6 }
    }

    @Test
    fun extensionFunctionClearStateStaticMock() {
        mockkStatic(Int::op)

        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify { 5 op 6 }

        mockkStatic(Int::op)

        verify(exactly = 0) { 5 op 6 }

        assertEquals(11, 5 op 6)

        every { 5 op 6 } returns 3

        assertEquals(3, 5 op 6)

        verify { 5 op 6 }
    }

    @Test
    fun extensionPropertyStaticMock() {
        mockkStatic(Int::selfOp)

        every { 5.selfOp } returns 2

        assertEquals(2, 5.selfOp)

        verify { 5.selfOp }
    }

    @Test
    fun extensionPropertyClearStateStaticMock() {
        mockkStatic(Int::selfOp)

        every { 5.selfOp } returns 2

        assertEquals(2, 5.selfOp)

        verify { 5.selfOp }

        mockkStatic(Int::selfOp)

        verify(exactly = 0) { 5.selfOp }

        assertEquals(25, 5.selfOp)

        every { 5.selfOp } returns 3

        assertEquals(3, 5.selfOp)

        verify { 5.selfOp }
    }

}
