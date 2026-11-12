package io.mockk.it

import io.mockk.MockKException
import io.mockk.clearStaticMockk
import io.mockk.confirmVerified
import io.mockk.declaringKotlinFile
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

val Int.selfOp get() = this * this

infix fun Int.oper(b: Int) = this + b

val topLevelProperty: Int get() = 77

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

    /**
     * GitHub issue #1510
     */
    @Test
    fun confirmVerifiedWithStaticMockFunctionReference() {
        mockkStatic(Int::op)
        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify(exactly = 1) { 5 op 6 }
        confirmVerified(Int::op)
    }

    /**
     * GitHub issue #1510
     */
    @Test
    fun confirmVerifiedResolvesCallableReferenceInVarargArgs() {
        mockkStatic(Int::op)
        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify(exactly = 1) { 5 op 6 }

        val references = arrayOf<Any>(Int::op)
        confirmVerified(*references)
    }

    /**
     * GitHub issue #1510
     */
    @Test
    fun confirmVerifiedResolvesCallableReferenceInVarargArgsWithClear() {
        mockkStatic(Int::op)
        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify(exactly = 1) { 5 op 6 }

        val references = arrayOf<Any>(Int::op)
        confirmVerified(*references, clear = true)

        verify(exactly = 0) { 5 op 6 }
    }

    /**
     * GitHub issue #1510, note 2
     */
    @Test
    fun confirmVerifiedResolvesTopLevelPropertyReferenceInVarargArgs() {
        mockkStatic(Int::selfOp)
        every { 5.selfOp } returns 2

        assertEquals(2, 5.selfOp)

        verify(exactly = 1) { 5.selfOp }

        val references = arrayOf<Any>(Int::selfOp)
        confirmVerified(*references)
    }

    /**
     * GitHub issue #1510, note 2
     */
    @Test
    fun confirmVerifiedResolvesPlainTopLevelPropertyReferenceInVarargArgs() {
        mockkStatic(::topLevelProperty)
        every { topLevelProperty } returns 2

        assertEquals(2, topLevelProperty)

        verify(exactly = 1) { topLevelProperty }

        val references = arrayOf<Any>(::topLevelProperty)
        confirmVerified(*references)
    }

    /**
     * GitHub issue #1510
     */
    @Test
    fun confirmVerifiedUnmockedCallableReferenceFailsWithClearError() {
        val functionReference = arrayOf<Any>(Int::unmockedOp)
        val functionException =
            assertFailsWith<MockKException> {
                confirmVerified(*functionReference)
            }
        assertContains(functionException.message!!, "can't find stub")

        val propertyReference = arrayOf<Any>(::unmockedTopLevelProperty)
        val propertyException =
            assertFailsWith<MockKException> {
                confirmVerified(*propertyReference)
            }
        assertContains(propertyException.message!!, "can't find stub")
    }

    /**
     * GitHub issue #1510
     */
    @Test
    fun confirmVerifiedWithDeclaringKotlinFileStillWorks() {
        mockkStatic(Int::op)
        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify(exactly = 1) { 5 op 6 }
        confirmVerified(Int::op.declaringKotlinFile)
    }
}
