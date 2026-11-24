package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * `withArg` blocks that assert on reified generic arguments of the same erased class.
 *
 * `ParentImpl<Child1>` and `ParentImpl<Child2>` erase to the same `ParentImpl` class, so a
 * `shouldBeInstanceOf`-style check passes for both calls and the user assertion only fails
 * later, with a `ClassCastException`, while the matcher inspects a call it is not meant for.
 * Such a failure from the assertion block is a non-match of that particular call, exactly like
 * a failing assertion, and must not abort verification with the erased-types hint (issue
 * #1279).
 */
class Issue1279GenericWithArgCastTest {
    sealed interface Parent<T : Any> {
        val child: T
    }

    data class ParentImpl<T : Any>(
        override val child: T,
    ) : Parent<T>

    data class Child1(val field: String)

    data class Child2(val field: Int)

    interface SomeService {
        fun myFun(event: Parent<*>)
    }

    class OtherService(
        val someService: SomeService,
    ) {
        fun myOtherFun() {
            someService.myFun(ParentImpl(Child1("abc")))
            someService.myFun(ParentImpl(Child2(123)))
        }
    }

    private inline fun <reified T : Parent<*>> SomeService.verifyStuff(crossinline block: (T) -> Unit) {
        verify {
            myFun(
                withArg(assertionErrorLogger = {}) { arg ->
                    @Suppress("UNCHECKED_CAST")
                    block(arg as T)
                },
            )
        }
    }

    @Test
    fun funTest() {
        val mock = mockk<SomeService> {
            every { myFun(any()) } returns Unit
        }

        OtherService(mock).myOtherFun()

        mock.verifyStuff<ParentImpl<Child1>> { assertEquals("abc", it.child.field) }
        mock.verifyStuff<ParentImpl<Child2>> { assertEquals(123, it.child.field) }
    }

    @Test
    fun funTestNoMatchReportsVerificationFailure() {
        val mock = mockk<SomeService> {
            every { myFun(any()) } returns Unit
        }

        mock.myFun(ParentImpl(Child2(123)))

        val ex =
            assertFailsWith<Throwable> {
                mock.verifyStuff<ParentImpl<Child1>> { assertEquals("abc", it.child.field) }
            }
        assertTrue(
            ex.message!!.contains("Verification failed"),
            "Expected a verification failure listing the recorded call, got: ${ex.message}",
        )
        assertTrue(
            !ex.message!!.contains("Class cast exception happened"),
            "Verification must not fail with the erased-types hint, got: ${ex.message}",
        )
    }
}
