package io.mockk.gh

import io.mockk.every
import io.mockk.mockkClass
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue31Test {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

    val mock = mockkClass(MockCls::class)

    @Test
    fun exactlyZeroWithAny() {
        every { mock.op(3, 4) } returns 5

        assertEquals(5, mock.op(3, 4))

        verify { mock.op(3, 4) }
    }
}