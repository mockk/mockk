package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class CapturingTest {

    class Cls {
        var value = 0
    }

    class MockCls {
        fun op(a: Int, b: Int, c: Cls) = a + b + c.value
    }

    class CoMockCls {
        suspend fun op(a: Int, b: Int, c: Cls) = a + b + c.value
    }

    @Test
    fun captureRegularSlot() {
        val mock = mockk<MockCls>()

        val slot = CapturingSlot<Cls>()
        every { mock.op(1, 2, capture(slot)) } returns 22

        assertEquals(22, mock.op(1, 2, Cls().apply { value = 55 }))
        assertEquals(55, slot.captured.value)

        verify { mock.op(1, 2, any()) }
    }

    @Test
    fun captureListSlot() {
        val mock = mockk<MockCls>()

        val items = mutableListOf<Cls>()
        every { mock.op(1, 2, capture(items)) } returns 22

        assertEquals(22, mock.op(1, 2, Cls().apply { value = 55 }))
        assertEquals(55, items[0].value)

        verify { mock.op(1, 2, any()) }
    }

    @Test
    fun coCaptureRegularSlot() {
        val mock = mockk<CoMockCls>()

        val slot = CapturingSlot<Cls>()
        coEvery { mock.op(1, 2, capture(slot)) } returns 22

        InternalPlatformDsl.runCoroutine {
            assertEquals(22, mock.op(1, 2, Cls().apply { value = 55 }))
        }
        assertEquals(55, slot.captured.value)

        coVerify { mock.op(1, 2, any()) }
    }

    @Test
    fun coCaptureListSlot() {
        val mock = mockk<CoMockCls>()

        val items = mutableListOf<Cls>()
        coEvery { mock.op(1, 2, capture(items)) } returns 22

        InternalPlatformDsl.runCoroutine {
            assertEquals(22, mock.op(1, 2, Cls().apply { value = 55 }))
        }
        assertEquals(55, items[0].value)

        coVerify { mock.op(1, 2, any()) }
    }
}