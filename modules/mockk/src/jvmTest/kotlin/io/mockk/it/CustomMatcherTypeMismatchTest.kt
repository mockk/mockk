package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class CustomMatcherTypeMismatchTest {
    open class Parent(
        val a: Int = 0,
    )

    class Child1(
        a: Int = 0,
    ) : Parent(a) {
        val c1 = "c1"
    }

    class User {
        fun f(p: Parent): Int = p.a
    }

    private fun mockUser() =
        mockk<User> {
            every { f(any()) } returns 0
        }

    @Test
    fun typedCustomMatcherAgainstDifferentlyTypedRecordedArgumentReportsTypeMismatch() {
        val mockUser = mockUser()
        val parent = Parent()

        mockUser.f(parent)

        val failure =
            assertFailsWith<AssertionError> {
                verify(exactly = 1) {
                    mockUser.f(
                        match<Child1> { it.c1 == "c1" },
                    )
                }
            }
        val message = failure.message!!

        assertContains(message, "argument type mismatch: expected Child1, actual Parent")
        assertFalse(message.contains("Class cast exception happened"), "Message: $message")
    }

    @Test
    fun typedCustomMatcherAgainstCorrectlyTypedArgumentStillReportsNoMatch() {
        val mockUser = mockUser()

        mockUser.f(Child1())

        val failure =
            assertFailsWith<AssertionError> {
                verify(exactly = 1) {
                    mockUser.f(
                        match<Child1> { it.c1 == "other value" },
                    )
                }
            }
        val message = failure.message!!

        assertContains(message, "result: -")
        assertFalse(message.contains("argument type mismatch"), "Message: $message")
    }
}
