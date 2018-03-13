package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.MockKException
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.impl.annotations.SpyK
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnnotationsTest {
    class MockCls {
        fun op(a: Int): Int = a + 1
    }

    class AnnotatedCls {
        @MockK
        lateinit var mock: MockCls

        @RelaxedMockK
        lateinit var relaxedMock: MockCls

        @SpyK
        var spy = MockCls()
    }

    class ReadonlyAnnotatedCls {
        @MockK
        val mock: MockCls? = null
    }

    @Test
    fun mock() {
        val obj = AnnotatedCls()

        MockKAnnotations.init(obj)

        assertFailsWith(MockKException::class) {
            obj.mock.op(1)
        }

        every { obj.mock.op(5) } returns 4

        assertEquals(4, obj.mock.op(5))

        verify { obj.mock.op(5) }
    }

    @Test
    fun relaxedMock() {
        val obj = AnnotatedCls()

        MockKAnnotations.init(obj)

        assertEquals(0, obj.relaxedMock.op(5))

        verify { obj.relaxedMock.op(5) }
    }

    @Test
    fun spy() {
        val obj = AnnotatedCls()

        MockKAnnotations.init(obj)

        assertEquals(6, obj.spy.op(5))

        verify { obj.spy.op(5) }
    }

    @Test
    fun readonlyError() {
        val obj = ReadonlyAnnotatedCls()


        assertFailsWith(MockKException::class) {
            MockKAnnotations.init(obj)
        }
    }
}