package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("UNUSED_PARAMETER")
class RelaxedMockingTest {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
        fun opUnit(a: Int, b: Int) {}
    }

    @Test
    fun rurfRegularOperationOk() {
        val mock = mockk<MockCls>(relaxUnitFun = true) {
            every { op(1, 2) } returns 4
        }

        assertEquals(4, mock.op(1, 2))
    }

    @Test
    fun rurfRegularOperationFail() {
        val mock = mockCls()

        assertFailsWith<MockKException> {
            assertEquals(4, mock.op(1, 2))
        }
    }

    @Test
    fun rurfUnitOperationOk() {
        val mock = mockCls()

        mock.opUnit(1, 2)
    }

    @Test
    fun rurfUnitOperationMocked() {
        val mock = mockCls()

        val slot = slot<Int>()
        every { mock.opUnit(1, capture(slot)) } just Runs

        mock.opUnit(1, 2)

        assertTrue(slot.isCaptured)
        assertEquals(2, slot.captured)
    }

    @Test
    fun testRelaxedFunction() {
        val block = mockk<() -> Unit>(relaxed = true)
        block()
        verify { block.invoke() }
    }

    private fun mockCls() = mockk<MockCls>(relaxUnitFun = true)
}
