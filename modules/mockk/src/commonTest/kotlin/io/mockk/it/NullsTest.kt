package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NullsTest {
    interface Wrapper
    data class IntWrapper(val data: Int) : Wrapper

    class MockCls {
        fun op(a: Wrapper?, b: Wrapper?): Int? {
            return if (a is IntWrapper && b is IntWrapper) {
                a.data + b.data
            } else {
                0
            }
        }
    }


    @MockK
    lateinit var mock: MockCls

    @BeforeTest
    fun init() {
        MockKAnnotations.init(this)
    }

    @Test
    fun isNull() {
        every { mock.op(null, isNull()) } returns 4

        assertEquals(4, mock.op(null, null))

        verify { mock.op(isNull(), null) }
    }

    @Test
    fun returnsNull() {
        every { mock.op(IntWrapper(1), IntWrapper(2)) } returns null

        assertEquals(null, mock.op(IntWrapper(1), IntWrapper(2)))

        verify { mock.op(IntWrapper(1), IntWrapper(2)) }

    }
}