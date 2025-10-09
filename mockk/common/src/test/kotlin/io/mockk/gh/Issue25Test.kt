package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Issue25Test {
    class MockCls {
        fun op(a: Int) = a + 1
        fun op2(a: Int, b: Int) = a + b
    }

    val mock = mockk<MockCls>()

    @Test
    fun exactlyZeroWithAny() {
        doCalls()

        verify(exactly = 0) {
            mock.op2(3, any())
            mock.op(3)
        }
    }

    fun doCalls() {
        every { mock.op(0) } throws RuntimeException("test")
        every { mock.op(1) } returnsMany listOf(1, 2, 3) andThen 5
        every { mock.op2(2, 1) } returns 3

        assertFailsWith(RuntimeException::class) {
            mock.op(0)
        }

        assertEquals(1, mock.op(1))
        assertEquals(2, mock.op(1))
        assertEquals(3, mock.op(1))
        assertEquals(5, mock.op(1))
        assertEquals(3, mock.op2(2, 1))
    }
}