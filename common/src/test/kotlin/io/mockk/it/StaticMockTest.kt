package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

infix fun Int.op(b: Int) = this + b

class StaticMockTest {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

    @Test
    fun simpleStaticMock() {
        mockkStatic("io.mockk.it.StaticMockTestKt")

        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify { 5 op 6 }
    }

    @Test
    fun clearStateStaticMock() {
        mockkStatic("io.mockk.it.StaticMockTestKt")

        every { 5 op 6 } returns 2

        assertEquals(2, 5 op 6)

        verify { 5 op 6 }

        mockkStatic("io.mockk.it.StaticMockTestKt")

        verify(exactly = 0) { 5 op 6 }

        assertEquals(11, 5 op 6)

        every { 5 op 6 } returns 3

        assertEquals(3, 5 op 6)

        verify { 5 op 6 }
    }

    @Suppress("DEPRECATION")
    @Test
    fun compatibilityDisjointMocking() {
        staticMockk<MockCls>().mock()
        staticMockk<MockCls>().unmock()
    }

}