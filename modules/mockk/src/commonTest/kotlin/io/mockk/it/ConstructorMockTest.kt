package io.mockk.it

import io.mockk.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConstructorMockTest {
    class ExampleClass {
        val exampleProperty: Int = 1
    }

    data class MockCls(private val x: Int = 0) {

        constructor(x: String) : this(x.toInt())

        fun op(a: Int, b: Int) = a + b + x

        fun opList(a: Int, b: Int) = listOf(a, b)

        fun chainOp(a: Int, b: Int) = MockCls(a + b + x)
    }

    @Test
    fun test1() {
        mockkConstructor(ExampleClass::class)

        every { anyConstructed<ExampleClass>().exampleProperty } returns 0

        assertEquals(0, ExampleClass().exampleProperty)
    }

    @Test
    fun unmockkAllTest() {
        mockkConstructor(ExampleClass::class)
        mockkConstructor(ExampleClass::class)

        every { anyConstructed<ExampleClass>().exampleProperty } returns 0

        assertEquals(0, ExampleClass().exampleProperty)

        unmockkAll()

        // mockkConstructor called multiple times, but unmockkAll should still be able to unmock it
        assertEquals(1, ExampleClass().exampleProperty)

        // Constructor not mocked -> MockkException
        assertFailsWith<MockKException> {
            every { anyConstructed<ExampleClass>().exampleProperty } returns 0
        }
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
    @Ignore // TODO fix verify bug an anyConstructed https://github.com/mockk/mockk/issues/1224
    fun clear() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        assertEquals(4, MockCls().op(1, 2))

        verify { anyConstructed<MockCls>().op(1, 2) }

        clearConstructorMockk(MockCls::class)

        verify(exactly = 0) { anyConstructed<MockCls>() }

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
    fun fakeConstructor2() {
        mockkConstructor(MockCls::class)

        every { MockCls(5).op(1, 2) } returns 4

        assertEquals(4, MockCls(5).op(1, 2))

        verify { MockCls(5).op(1, 2) }
    }

    @Test
    fun constructedWith() {
        mockkConstructor(MockCls::class)

        every {
            constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).op(any(), any())
        } returns 23
        every {
            constructedWith<MockCls>(EqMatcher(6)).op(any(), any())
        } returns 55
        every {
            constructedWith<MockCls>(OfTypeMatcher<Int>(Int::class)).op(any(), any())
        } returns 35

        assertEquals(23, MockCls("5").op(1, 2))
        assertEquals(35, MockCls(5).op(1, 2))
        assertEquals(55, MockCls(6).op(1, 2))

        verify {
            constructedWith<MockCls>(EqMatcher(6)).op(1, 2)
        }
    }

    @Test
    fun unmockkAllconstructedWith() {
        mockkConstructor(MockCls::class)
        mockkConstructor(MockCls::class)

        val checkConstructedWith = { a: Int, b: Int, c: Int ->
            every {
                constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).op(any(), any())
            } returns a
            every {
                constructedWith<MockCls>(EqMatcher(6)).op(any(), any())
            } returns b
            every {
                constructedWith<MockCls>(OfTypeMatcher<Int>(Int::class)).op(any(), any())
            } returns c

            assertEquals(a, MockCls("5").op(1, 2))
            assertEquals(b, MockCls(6).op(1, 2))
            assertEquals(c, MockCls(5).op(1, 2))
        }

        checkConstructedWith(23, 55, 35)

        // New mockkConstructor -> we can still mock as expected
        mockkConstructor(MockCls::class)
        checkConstructedWith(44, 101, 42)

        // mockkConstructor was called multiple times, but we can still unmock it via unmockkAll
        unmockkAll()

        assertEquals(8, MockCls("5").op(1, 2))
        assertEquals(8, MockCls(5).op(1, 2))
        assertEquals(9, MockCls(6).op(1, 2))

        // Constructor not mocked anymore -> MockkException expected
        assertFailsWith<MockKException> {
            every {
                constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).op(any(), any())
            } returns 23
        }
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
