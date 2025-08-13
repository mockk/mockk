package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MockKStubTest {
    @Test
    fun givenAMockkStubWithAnswerConfiguredWhenCallingItWithOtherParametersThenTheExceptionContainsConfiguredAnswers() {
        val mock: DummyClass = mockk()
        val expectedMessage = "no answer found for $mock.function(3) among the configured answers: ($mock.function(eq(2))))"

        every {
            mock.function(2)
        } returns 3

        val exception = assertThrows<MockKException> {
            mock.function(3)
        }

        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun givenAMockkStubWithAnswersConfiguredWhenCallingItWithOtherParametersThenTheExceptionContainsConfiguredAnswers() {
        val mock: DummyClass = mockk()
        val expectedMessage = """no answer found for $mock.function(3) among the configured answers: ($mock.function(eq(2)))
$mock.function(eq(5))))"""

        every {
            mock.function(2)
        } returns 3
        every {
            mock.function(5)
        } returns 3

        val exception = assertThrows<MockKException> {
            mock.function(3)
        }

        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testNull() {
        val mock: DummyClass = mockk()

        every {
            mock.functionNull(anyNullable<Int?>())
        } returns 3

        mock.functionNull(null)
    }


    class DummyClass {
        fun function(a: Int) = a
        fun functionNull(a: Any?) = 2
    }
}