package io.mockk.ait

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class PrePTest {
    open class MockCls {
        open fun sum(a: Int, b: Int) = a + b
    }

    @Test
    fun testMock() {
        val mock = mockk<MockCls>()

        every { mock.sum(1, 2) } returns 4

        assertEquals(4, mock.sum(1, 2))
    }
}