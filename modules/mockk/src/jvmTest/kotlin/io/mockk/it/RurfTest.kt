package io.mockk.it

import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RurfTest {
    @MockK(relaxUnitFun = true)
    lateinit var mock1: RurfMockCls

    @MockK
    lateinit var mock2: RurfMockCls

    @Test
    fun rurfRegularOperationOk() {
        val mock = mockk<RurfMockCls>(relaxUnitFun = true) {
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

    private fun mockCls() = mockk<RurfMockCls>(relaxUnitFun = true)

    @Test
    fun rurfUnitOperationOkAnnotation1() {
        MockKAnnotations.init(this)
        mock1.opUnit(1, 2)
    }

    @Test
    fun rurfUnitOperationOkAnnotation2() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mock2.opUnit(1, 2)
    }

}