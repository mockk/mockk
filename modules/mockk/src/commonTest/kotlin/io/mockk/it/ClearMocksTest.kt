package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

fun topLevelFn() = 55

class ClearMocksTest {
    class MockCls {
        fun op(a: Int) = a + 1
    }

    val mock = spyk(MockCls())

    @Test
    fun clearAnswers() {
        every { mock.op(1) } returns 3
        assertEquals(3, mock.op(1))
        verify { mock.op(1) }
        clearMocks(mock, answers = true, recordedCalls = false, childMocks = false)
        assertEquals(2, mock.op(1))
        verify { mock.op(1) }
    }

    @Test
    fun clearRecordedCalls() {
        every { mock.op(1) } returns 3
        assertEquals(3, mock.op(1))
        verify { mock.op(1) }
        clearMocks(mock, answers = false, recordedCalls = true, childMocks = false)
        verify(inverse = true) { mock.op(1) }
        assertEquals(3, mock.op(1))
    }

    @Test
    fun clearChildMocks() {
        every { mock.op(1) } returns 3
        assertEquals(3, mock.op(1))
        verify { mock.op(1) }
        clearMocks(mock, answers = false, recordedCalls = true, childMocks = false)
        verify(inverse = true) { mock.op(1) }
        assertEquals(3, mock.op(1))
    }

    @Test
    fun clearAll() {
        val obj = MockCls()
        mockkObject(obj)
        mockkStatic("io.mockk.it.ClearMocksTestKt")
        mockkConstructor(MockCls::class)

        every { mock.op(1) } returns 11
        every { anyConstructed<MockCls>().op(2) } returns 22
        every { topLevelFn() } returns 33
        every { obj.op(4) } returns 44

        assertEquals(11, mock.op(1))
        assertEquals(22, MockCls().op(2))
        assertEquals(33, topLevelFn())
        assertEquals(44, obj.op(4))

        clearAllMocks()

        assertEquals(2, mock.op(1))
        assertEquals(3, MockCls().op(2))
        assertEquals(55, topLevelFn())
        assertEquals(5, obj.op(4))
    }

    @Test
    fun clearAllMocksCurrentThreadOnly() {
        var mockInOtherThread: MockCls? = null

        val thread = Thread {
            mockInOtherThread = mockk()
            every { mockInOtherThread!!.op(any()) } returns 42
        }
        thread.start()
        thread.join()

        every { mock.op(any()) } returns 24
        assertEquals(24, mock.op(1))
        assertEquals(42, mockInOtherThread?.op(1))

        clearAllMocks(currentThreadOnly = true)

        // Current thread's mock is cleared;
        assertEquals(2, mock.op(1))
        // The other thread's mock remains.
        assertEquals(42, mockInOtherThread?.op(1))
    }
}