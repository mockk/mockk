package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class MockkClassTest {

    val mock = mockkClass(MockCls::class)

    /**
     * See issue #31
     */
    @Test
    fun exactlyZeroWithAny() {
        every { mock.op(3, 4) } returns 5

        assertEquals(5, mock.op(3, 4))

        verify { mock.op(3, 4) }
    }

    private val targetClass = mockk<TestClass>()

    /**
     * See issue #158
     */
    @Test
    fun testNothingIsNotThrowingNPE() {
        every { targetClass.alwaysThrows() } answers {
            throw IllegalArgumentException("this is a test")
        }
    }

    class TestClass {
        fun alwaysThrows() : Nothing {
            throw RuntimeException("this can be any exception")
        }
    }

    class MockCls {
        fun op(a: Int, b: Int) = a + b
    }
}
