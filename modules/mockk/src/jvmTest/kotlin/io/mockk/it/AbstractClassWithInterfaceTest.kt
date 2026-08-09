package io.mockk.it

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Test related to GitHub issue: AbstractMethodError in every invocation
 *
 * When mocking abstract classes that implement interfaces, the methods from
 * the interface should be properly proxied to avoid AbstractMethodError.
 */
class AbstractClassWithInterfaceTest {

    interface A {
        fun something(param: String)
    }

    abstract class B : A {
        companion object {
            fun getInstance(): B = CImpl()
        }

        abstract fun otherthing(param: String)
    }

    class CImpl : B() {
        override fun otherthing(param: String) {
            // implementation
        }

        override fun something(param: String) {
            // implementation
        }
    }

    // Nested interfaces for testing interface inheritance
    interface Base {
        fun baseMethod(): String
    }

    interface Derived : Base {
        fun derivedMethod(): String
    }

    abstract class AbstractImpl : Derived {
        abstract fun abstractMethod(): String
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(B.Companion)
    }

    @Test
    fun `mocking abstract class implementing interface should work`() {
        mockkObject(B.Companion)

        val mockB = mockk<B>()
        every { B.getInstance() } returns mockB
        every { mockB.something(any()) } just runs
        every { mockB.otherthing(any()) } just runs

        // This should not throw AbstractMethodError
        B.getInstance().something("test")
        B.getInstance().otherthing("test")
    }

    @Test
    fun `mock with interface method should not throw AbstractMethodError`() {
        val mockB = mockk<B>()
        every { mockB.something(any()) } just runs
        every { mockB.otherthing(any()) } just runs

        // This should not throw AbstractMethodError
        mockB.something("test")
        mockB.otherthing("test")
    }

    @Test
    fun `mock of abstract class with nested interfaces should work`() {
        val mock = mockk<AbstractImpl>()
        every { mock.baseMethod() } returns "base"
        every { mock.derivedMethod() } returns "derived"
        every { mock.abstractMethod() } returns "abstract"

        assertEquals("base", mock.baseMethod())
        assertEquals("derived", mock.derivedMethod())
        assertEquals("abstract", mock.abstractMethod())
    }
}
