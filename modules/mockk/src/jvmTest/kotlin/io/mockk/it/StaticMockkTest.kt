package io.mockk.it

import io.mockk.clearStaticMockk
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

val Int.selfOp get() = this * this

infix fun Int.oper(b: Int) = this + b

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

    @Test
    fun confirmVerifiedToWorkWithStaticMockFunction() {
        mockkStatic(Int::oper)
        every { 5 oper 6 } returns 2

        assertEquals(2, 5 oper 6)

        verify(exactly = 1) { 5 oper 6 }
        confirmVerified(Int::oper)
    }

    @Test
    fun confirmVerifiedToWorkWithStaticMockFunctionAndClear() {
        mockkStatic(Int::oper)
        every { 5 oper 6 } returns 2

        assertEquals(2, 5 oper 6)

        verify(exactly = 1) { 5 oper 6 }
        confirmVerified(Int::oper, clear = true)

        verify(exactly = 0) { 5 oper 6 }
    }

    @Test
    fun confirmVerifiedToWorkWithStaticMockProperty() {
        mockkStatic(Int::selfOp)
        every { 5.selfOp } returns 2

        assertEquals(2, 5.selfOp)

        verify(exactly = 1) { 5.selfOp }
        confirmVerified(Int::selfOp)
    }

    @Test
    fun confirmVerifiedToWorkWithStaticMockPropertyAndClear() {
        mockkStatic(Int::selfOp)
        every { 5.selfOp } returns 2

        assertEquals(2, 5.selfOp)

        verify(exactly = 1) { 5.selfOp }
        confirmVerified(Int::selfOp, clear = true)

        verify(exactly = 0) { 5.selfOp }
    }

    @Test
    fun confirmVerifiedFunctionClearKeepsSiblingCalls() {
        mockkStatic(Int::oper)
        mockkStatic(Int::selfOp)
        every { 5 oper 6 } returns 2
        every { 5.selfOp } returns 99

        assertEquals(2, 5 oper 6)
        assertEquals(99, 5.selfOp)

        verify(exactly = 1) { 5 oper 6 }
        confirmVerified(Int::oper, clear = true)

        verify(exactly = 0) { 5 oper 6 }
        assertFailsWith<AssertionError> {
            confirmVerified(Int::selfOp)
        }
    }

    @Test
    fun confirmVerifiedKFunctionIsScopedToSelectedFunction() {
        mockkStatic(Int::oper)
        mockkStatic(Int::selfOp)
        every { 5 oper 6 } returns 2
        every { 5.selfOp } returns 99

        assertEquals(2, 5 oper 6)
        assertEquals(99, 5.selfOp)

        verify(exactly = 1) { 5 oper 6 }
        confirmVerified(Int::oper)

        assertFailsWith<AssertionError> {
            confirmVerified(Int::selfOp)
        }
    }
}
