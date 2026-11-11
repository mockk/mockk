package io.mockk.core

import arrow.core.NonEmptySet
import arrow.core.nonEmptySetOf
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

// https://github.com/mockk/mockk/issues/1360
class ArrowNonEmptySetParamTest {
    interface SomeInterface {
        fun someFunction(something: NonEmptySet<String>): String
    }

    class SomeClass : SomeInterface {
        override fun someFunction(something: NonEmptySet<String>): String = something.reduce { acc, s -> acc + s }
    }

    @Test
    fun `given interface method taking NonEmptySet when interface is mocked then call is intercepted`() {
        val someService =
            mockk<SomeInterface> {
                every { someFunction(any()) } returns "abc"
            }
        assertEquals("abc", someService.someFunction(nonEmptySetOf("a", "b", "c")))
    }

    @Test
    fun `given class method taking NonEmptySet when class is mocked then call is intercepted`() {
        val someService =
            mockk<SomeClass> {
                every { someFunction(any()) } returns "abc"
            }
        assertEquals("abc", someService.someFunction(nonEmptySetOf("a", "b", "c")))
    }
}
