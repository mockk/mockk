package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstructorMockTest {
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
    fun cleanAfter() {
        mock.use {
            every { anyConstructed<MockCls>().op(1, 2) } returns 4
        }

        assertEquals(3, MockCls().op(1, 2))
    }

    @Test
    fun instanceCleanAfter() {
        val instance = mock.use {
            every { anyConstructed<MockCls>().op(1, 2) } returns 4

            MockCls()
        }

        assertEquals(3, instance.op(1, 2))
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

    @Test
    fun scope() {
        mock.use {
            every { anyConstructed<MockCls>().op(1, 2) } returns 4

            constructorMockk<MockCls>().use {
                every { anyConstructed<MockCls>().op(1, 2) } returns 5

                assertEquals(5, MockCls().op(1, 2))
            }

            assertEquals(4, MockCls().op(1, 2))
        }
    }

    @Test
    fun fakeConstructor() {
        mock.use {
            every { MockCls().op(1, 2) } returns 4

            assertEquals(4, MockCls().op(1, 2))

            verify { MockCls().op(1, 2) }
        }
    }
}