package io.mockk.it

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

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
}