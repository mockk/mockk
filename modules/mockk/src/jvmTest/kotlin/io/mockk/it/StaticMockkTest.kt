package io.mockk.it

import io.mockk.clearStaticMockk
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.time.Instant
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

val Int.selfOp get() = this * this

class StaticMockkTest {
    /**
     * GitHub issue #92
     */
    @Test
    fun staticMockkJavaFunction() {
        val epochSeconds = 123L
        mockkStatic(Instant::class)
        every { Instant.now().epochSecond } returns epochSeconds

        assertEquals(123L, Instant.now().epochSecond)
    }

    /**
     * GitHub issue #99
     */
    @Test
    fun unmockStatic_unmocksStaticMocks() {
        mockkStatic(Instant::class)
        every { Instant.now().toEpochMilli() } returns 123L

        assertEquals(123L, Instant.now().toEpochMilli())

        unmockkStatic(Instant::class)

        assertNotEquals(123L, Instant.now().toEpochMilli())
    }

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
    fun extensionFunctionClearStaticMock() {
        mockkStatic(Int::op)

        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        clearStaticMockk(Int::op)

        verify(exactly = 0) { 5 op 6 }
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

    @Test
    fun extensionPropertyClearStaticMock() {
        mockkStatic(Int::selfOp)

        every { 5.selfOp } returns 2

        assertEquals(2, 5.selfOp)

        clearStaticMockk(Int::selfOp)

        verify(exactly = 0) { 5.selfOp }
    }
}
