package io.mockk.gh

import io.mockk.classMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Issue31Test {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

    val mock = classMockk(MockCls::class)

    @Test
    fun exactlyZeroWithAny() {
        every { mock.op(3, 4) } returns 5

        assertEquals(5, mock.op(3, 4))

        verify { mock.op(3, 4) }
    }
}