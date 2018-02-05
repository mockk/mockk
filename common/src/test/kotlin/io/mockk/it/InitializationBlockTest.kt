package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertEquals

class InitializationBlockTest {
    @Test
    fun initializeMockWithInitializationBlock() {
        val mock: MockCls = mockk {
            every { op(1, 2) } returns 5
        }

        assertEquals(5, mock.op(1, 2))
    }

    @Test
    fun initializeSpyWithInitializationBlock() {
        val mock: MockCls = spyk {
            every { op(1, 2) } returns 5
        }

        assertEquals(5, mock.op(1, 2))
        assertEquals(4, mock.op(2, 2))
    }

    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }
}