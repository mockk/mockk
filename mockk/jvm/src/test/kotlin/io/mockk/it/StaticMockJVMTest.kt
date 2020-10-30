package io.mockk.it

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticMockJVMTest {

    @Test
    fun simpleStaticMock() {
        mockkStatic(Int::op)

        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify { 5 op 6 }
    }

    @Test
    fun clearStateStaticMock() {
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

}
