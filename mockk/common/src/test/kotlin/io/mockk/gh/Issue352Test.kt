package io.mockk.gh

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Issue352Test {

    open class MockedSubject {
        open fun doSomething(id: String?, data: Any?): String {
            throw IllegalStateException("Not mocked :(")
        }
    }

    private val mock = mockk<MockedSubject>()

    @BeforeTest
    fun setup() {
        every { mock.doSomething("1", "data1") } returns "result1"
        every { mock.doSomething("2", "data2") } returns "result2"
    }

    @Test
    fun `It throws a MockkException when verifying the same function twice with slots`() {
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

    @Test
    fun `It does not throw a MockkException when there are multiple tests verifying with slots`() {
        mock.doSomething("1", "data1")

        val slot = slot<String>()
        verify {
            mock.doSomething("1", capture(slot))
        }

        assertEquals("data1", slot.captured)
    }

    @Test
    fun `Another test to test the coexistence of tests with slots`() {
        mock.doSomething("1", "data1")

        val slot = slot<String>()
        verify {
            mock.doSomething("1", capture(slot))
        }

        assertEquals("data1", slot.captured)
    }

    @Test
    fun `It allows multiple capturings of the same function using a mutableList`() {
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

}
