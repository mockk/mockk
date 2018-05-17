package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstructorTest {
    val mock = constructorMockk<MockCls>()

    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }

    @Test
    fun simpleTest() {
        mock.use {
            every { anyConstructed<MockCls>().op(1, 2) } returns 4

            assertEquals(4, MockCls().op(1, 2))

            verify { anyConstructed<MockCls>().op(1, 2) }
        }
    }

    @Test
    fun clear() {
        mock.use {
            every { anyConstructed<MockCls>().op(1, 2) } returns 4

            assertEquals(4, MockCls().op(1, 2))

            verify { anyConstructed<MockCls>().op(1, 2) }

            mock.clear()

            verify { anyConstructed<MockCls>() wasNot Called }

            assertEquals(3, MockCls().op(1, 2))

            verify { anyConstructed<MockCls>().op(1, 2) }
        }
    }
}