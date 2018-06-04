package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstructorMockTest {
    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

    @Test
    fun simpleTest() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        assertEquals(4, MockCls().op(1, 2))

        verify { anyConstructed<MockCls>().op(1, 2) }
    }

    @Test
    fun cleanAfter() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        clearConstructorMockk(MockCls::class)

        assertEquals(3, MockCls().op(1, 2))
    }

    @Test
    fun instanceCleanAfter() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        val instance = MockCls()

        unmockkConstructor(MockCls::class)

        assertEquals(3, instance.op(1, 2))
    }

    @Test
    fun clear() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        assertEquals(4, MockCls().op(1, 2))

        verify { anyConstructed<MockCls>().op(1, 2) }

        clearConstructorMockk(MockCls::class)

        verify { anyConstructed<MockCls>() wasNot Called }

        assertEquals(3, MockCls().op(1, 2))

        verify { anyConstructed<MockCls>().op(1, 2) }

    }

    @Test
    fun fakeConstructor() {
        mockkConstructor(MockCls::class)

        every { MockCls().op(1, 2) } returns 4

        assertEquals(4, MockCls().op(1, 2))

        verify { MockCls().op(1, 2) }
    }
}