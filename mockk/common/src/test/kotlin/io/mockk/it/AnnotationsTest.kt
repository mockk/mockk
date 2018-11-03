package io.mockk.it

import io.mockk.MockKAnnotations
import io.mockk.MockKException
import io.mockk.every
import io.mockk.impl.annotations.*
import io.mockk.verify
import kotlin.test.*

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

    class InjectionTarget1(val obj: MockCls)

    class InjectionTarget2 {
        lateinit var obj: MockCls
    }

    class InjectionTarget3{
        val obj = MockCls()
    }

    class InjectSourceCls {
        @MockK
        lateinit var mock: MockCls

        @InjectMockKs
        lateinit var target1: InjectionTarget1

        @InjectMockKs
        lateinit var target2: InjectionTarget2

        @OverrideMockKs
        lateinit var target3: InjectionTarget3
    }

    @Test
    fun inject() {
        val source = InjectSourceCls()

        MockKAnnotations.init(source)

        assertNotNull(source.mock)

        assertSame(source.mock, source.target1.obj)
        assertSame(source.mock, source.target2.obj)
        assertSame(source.mock, source.target3.obj)
    }
}