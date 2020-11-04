package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstructorMockTest {
    class ExampleClass {
        val exampleProperty: Int = 1
    }

    object ExampleObject {
        private val exampleClass = ExampleClass()
        fun getExampleProperty(): Int = exampleClass.exampleProperty
    }

    class MockCls(val x: Int = 0) {
        fun op(a: Int, b: Int) = a + b + x

        fun opList(a: Int, b: Int) = listOf(a, b)

        fun chainOp(a: Int, b: Int) = MockCls(a + b + x)
    }

    @Test
    fun test1() {
        mockkConstructor(ExampleClass::class)

        every { anyConstructed<ExampleClass>().exampleProperty } returns 0

        assertEquals(0, ExampleObject.getExampleProperty())
    }

    @Test
    fun test2() {
        mockkConstructor(ExampleClass::class)

        every { anyConstructed<ExampleClass>().exampleProperty } returns 0

        assertEquals(0, ExampleObject.getExampleProperty())
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

    @Test
    fun returnObject() {
        mockkConstructor(MockCls::class)

        every { MockCls().opList(1, 2) } returns listOf(5, 6)

        assertEquals(listOf(5, 6), MockCls().opList(1, 2))

        verify { MockCls().opList(1, 2) }
    }

    @Test
    fun chainedOps() {
        mockkConstructor(MockCls::class)

        every { MockCls().chainOp(1, 2).chainOp(3, 4).op(5, 6) } returns 7

        assertEquals(7, MockCls().chainOp(1, 2).chainOp(3, 4).op(5, 6))

        verify { MockCls().chainOp(1, 2).chainOp(3, 4).op(5, 6) }

        clearConstructorMockk(MockCls::class)

        assertEquals(21, MockCls().chainOp(1, 2).chainOp(3, 4).op(5, 6))
    }
}