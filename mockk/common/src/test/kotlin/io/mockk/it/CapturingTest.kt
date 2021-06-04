package io.mockk.it

import io.mockk.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CapturingTest {

    private val mock = mockk<MockedSubject>()

    @BeforeTest
    fun setup() {
        every { mock.doSomething("1", "data1") } returns "result1"
        every { mock.doSomething("2", "data2") } returns "result2"
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

    /**
     * See issue #352.
     */
    @Test
    fun itThrowsAMockkExceptionWhenVerifyingTheSameFunctionTwiceWithSlots() {
        mock.doSomething("1", "data1")
        mock.doSomething("2", "data2")

        val dataSlotId1 = slot<String>()
        val dataSlotId2 = slot<String>()

        assertFailsWith<MockKException> {
            verify {
                mock.doSomething("1", capture(dataSlotId1))
                mock.doSomething("2", capture(dataSlotId2))
            }
        }
    }

    /**
     * See issue #352.
     */
    @Test
    fun itDoesNotThrowAMockkExceptionWhenThereAreMultipleTestsVerifyingWithSlots() {
        mock.doSomething("1", "data1")

        val slot = slot<String>()
        verify {
            mock.doSomething("1", capture(slot))
        }

        assertEquals("data1", slot.captured)
    }

    /**
     * See issue #352.
     */
    @Test
    fun anotherTestToTestTheCoexistenceOfTestsWithSlots() {
        mock.doSomething("1", "data1")

        val slot = slot<String>()
        verify {
            mock.doSomething("1", capture(slot))
        }

        assertEquals("data1", slot.captured)
    }

    /**
     * See issue #352.
     */
    @Test
    fun itAllowsMultipleCapturingsOfTheSameFunctionUsingAMutableList() {
        mock.doSomething("1", "data1")
        mock.doSomething("2", "data2")

        val slotList = mutableListOf<String>()

        verify {
            mock.doSomething("1", capture(slotList))
            mock.doSomething("2", capture(slotList))
        }

        assertEquals("data1", slotList[0])
        assertEquals("data2", slotList[1])
    }

    class Cls {
        var value = 0
    }

    class MockCls {
        fun op(a: Int, b: Int, c: Cls) = a + b + c.value
    }

    class CoMockCls {
        suspend fun op(a: Int, b: Int, c: Cls) = a + b + c.value
    }

    open class MockedSubject {
        open fun doSomething(id: String?, data: Any?): String {
            throw IllegalStateException("Not mocked :(")
        }
    }
}
