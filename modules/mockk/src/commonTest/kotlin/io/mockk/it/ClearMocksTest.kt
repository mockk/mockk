package io.mockk.it

import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

fun topLevelFn() = 55

class ClearMocksTest {
    class MockCls {
        fun op(a: Int) = a + 1

        fun child(): MockCls = error("need a stub")
    }

    val spyObj = spyk(MockCls())
    val mockObj = mockk<MockCls>(relaxed = true)

    @Test
    fun clearAnswers() {
        every { spyObj.op(1) } returns 3
        assertEquals(3, spyObj.op(1))
        verify(exactly = 1) { spyObj.op(1) }

        clearMocks(spyObj, answers = true, recordedCalls = false, childMocks = false, verificationMarks = false)

        confirmVerified(spyObj, clear = true)
        assertEquals(2, spyObj.op(1))
        verify(exactly = 1) { spyObj.op(1) }
    }

    @Test
    fun `clearAnswers does not affect a child`() {
        val child = mockObj.child()
        every { child.op(1) } returns 3
        assertEquals(3, child.op(1))
        verify(exactly = 1) { child.op(1) }

        clearMocks(mockObj, answers = true, recordedCalls = false, childMocks = false, verificationMarks = false)

        confirmVerified(child, clear = true)
        assertEquals(3, child.op(1))
        verify(exactly = 1) { child.op(1) }
        assertSame(child, mockObj.child())
    }

    @Test
    fun clearRecordedCalls() {
        every { spyObj.op(1) } returns 3

        assertEquals(3, spyObj.op(1))
        verify(exactly = 1) { spyObj.op(1) }

        clearMocks(spyObj, answers = false, recordedCalls = true, childMocks = false, verificationMarks = false)

        verify(inverse = true) { spyObj.op(1) }
        assertEquals(3, spyObj.op(1))
    }

    @Test
    fun `clearRecordedCalls does not affect a child`() {
        val child = mockObj.child()
        every { child.op(1) } returns 3
        assertEquals(3, child.op(1))
        verify(exactly = 1) { child.op(1) }

        clearMocks(mockObj, answers = false, recordedCalls = true, childMocks = false, verificationMarks = false)

        confirmVerified(child, clear = true)
        assertEquals(3, child.op(1))
        verify(exactly = 1) { child.op(1) }
        assertSame(child, mockObj.child())
    }

    @Test
    fun clearChildMocks() {
        val child = mockObj.child()

        every { mockObj.op(1) } returns 3
        every { child.op(1) } returns 3

        assertEquals(3, mockObj.op(1))
        assertEquals(3, child.op(1))

        verify(exactly = 1) { mockObj.op(1) }
        verify(exactly = 1) { mockObj.child() }
        verify(exactly = 1) { child.op(1) }
        confirmVerified(mockObj, child)

        clearMocks(mockObj, answers = false, recordedCalls = false, childMocks = true, verificationMarks = false)

        confirmVerified(mockObj, child)

        assertEquals(3, mockObj.op(1))
        assertEquals(3, child.op(1))

        assertNotSame(child, mockObj.child())
    }

    @Test
    fun clearVerificationMarks() {
        every { spyObj.op(1) } returns 3
        assertEquals(3, spyObj.op(1))
        verify { spyObj.op(1) }

        clearMocks(spyObj, answers = false, recordedCalls = false, childMocks = false, verificationMarks = true)

        verify(exactly = 1) { spyObj.op(1) }
        confirmVerified(spyObj)
        assertEquals(3, spyObj.op(1))
    }

    @Test
    fun `clearVerificationMarks does not affect a child`() {
        val child = mockObj.child()
        every { child.op(1) } returns 3
        assertEquals(3, child.op(1))
        verify(exactly = 1) { child.op(1) }

        clearMocks(mockObj, answers = false, recordedCalls = false, childMocks = false, verificationMarks = true)

        confirmVerified(child, clear = true)
        assertEquals(3, child.op(1))
        verify(exactly = 1) { child.op(1) }
        assertSame(child, mockObj.child())
    }

    @Test
    fun clearAll() {
        val obj = MockCls()
        mockkObject(obj)
        mockkStatic("io.mockk.it.ClearMocksTestKt")
        mockkConstructor(MockCls::class)

        val child = mockObj.child()

        every { spyObj.op(1) } returns 11
        every { anyConstructed<MockCls>().op(2) } returns 22
        every { topLevelFn() } returns 33
        every { obj.op(4) } returns 44
        every { child.op(5) } returns 55

        assertEquals(11, spyObj.op(1))
        assertEquals(22, MockCls().op(2))
        assertEquals(33, topLevelFn())
        assertEquals(44, obj.op(4))
        assertEquals(55, child.op(5))

        clearAllMocks()

        assertEquals(2, spyObj.op(1))
        assertEquals(3, MockCls().op(2))
        assertEquals(55, topLevelFn())
        assertEquals(5, obj.op(4))
        assertEquals(0, child.op(5))
        assertNotSame(child, mockObj.child())
    }

    @Test
    fun clearAllMocksCurrentThreadOnly() {
        var mockInOtherThread: MockCls? = null

        val thread =
            Thread {
                mockInOtherThread = mockk()
                every { mockInOtherThread!!.op(any()) } returns 42
            }
        thread.start()
        thread.join()

        every { spyObj.op(any()) } returns 24
        assertEquals(24, spyObj.op(1))
        assertEquals(42, mockInOtherThread?.op(1))

        clearAllMocks(currentThreadOnly = true)

        // Current thread's mock is cleared;
        assertEquals(2, spyObj.op(1))
        // The other thread's mock remains.
        assertEquals(42, mockInOtherThread?.op(1))
    }
}
