package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression tests for issue #1198: since Kotlin 1.9.20 the compiler's const-evaluation phase
 * folds `MyEnum.Entry.name` on a statically known entry into a compile-time constant, including
 * inside `every { ... }` blocks. The mocked entry is instrumented correctly (capturing the entry
 * in a local variable defeats the folding and stubbing works), but with the static reference no
 * call reaches the mock, so the block records nothing and stubbing fails.
 *
 * MockK cannot restore the folded call at runtime, so it now detects this situation and
 * appends guidance to the "Missing mocked calls" error instead of leaving the generic
 * message that points at an unmocked object.
 *
 * `ordinal` is currently not const-evaluated in ordinary expression positions (unlike in
 * `const` contexts), so stubbing it through a static reference keeps recording a real call
 * and works unchanged.
 */
class Issue1198EnumConstEvalStubbingTest {
    enum class TestEnum {
        ALPHA,
        BETA,
    }

    object NotAnEnum {
        fun value(): Int = 1
    }

    private class Helper {
        fun value(): Int = 1
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun everyOnStaticallyReferencedEnumEntryNameExplainsConstEvaluation() {
        mockkObject(TestEnum.ALPHA)

        val exception =
            assertFailsWith<MockKException> {
                every { TestEnum.ALPHA.name } returns "not-alpha"
            }

        assertEnumConstEvaluationNote(exception)
    }

    @Test
    fun everyOnStaticallyReferencedEnumEntryOrdinalStubsTheEntry() {
        mockkObject(TestEnum.ALPHA)

        every { TestEnum.ALPHA.ordinal } returns 42

        assertEquals(42, TestEnum.ALPHA.ordinal)
    }

    @Test
    fun missingCallsWithNonEnumObjectMockKeepsTheGenericMessage() {
        mockkObject(NotAnEnum)

        val exception =
            assertFailsWith<MockKException> {
                every { Helper().value() } returns 22
            }

        val message = requireNotNull(exception.message)
        assertTrue(
            message.startsWith(MISSING_CALLS_MESSAGE),
            "Unexpected exception: $message",
        )
        assertFalse(
            message.contains("enum entry referenced statically"),
            "Enum const-evaluation note must not appear when no enum object mock is active:\n$message",
        )
    }

    @Test
    fun everyOnEnumEntryNameCapturedInLocalVariableStubsTheEntry() {
        mockkObject(TestEnum.ALPHA)

        val entry = TestEnum.ALPHA
        every { entry.name } returns "not-alpha"

        assertEquals("not-alpha", entry.name)
    }

    @Test
    fun everyOnEnumEntryOrdinalCapturedInLocalVariableStubsTheEntry() {
        mockkObject(TestEnum.ALPHA)

        val entry = TestEnum.ALPHA
        every { entry.ordinal } returns 42

        assertEquals(42, entry.ordinal)
    }

    private fun assertEnumConstEvaluationNote(exception: MockKException) {
        val message = requireNotNull(exception.message)
        assertTrue(
            message.startsWith(MISSING_CALLS_MESSAGE),
            "Unexpected exception: $message",
        )
        assertTrue(
            message.contains("enum entry referenced statically"),
            "Enum const-evaluation note is missing from the error message:\n$message",
        )
        assertTrue(
            message.contains("Kotlin compiler since 1.9.20 evaluates it to a constant"),
            "Const-evaluation explanation is missing from the error message:\n$message",
        )
        assertTrue(
            message.contains("local variable"),
            "Workaround guidance is missing from the error message:\n$message",
        )
    }

    private companion object {
        const val MISSING_CALLS_MESSAGE =
            "Missing mocked calls inside every { ... } block: make sure the object inside the block is a mock"
    }
}
