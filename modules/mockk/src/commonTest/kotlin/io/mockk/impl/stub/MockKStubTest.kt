package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MockKStubTest {
    @Test
    fun givenAMockkStubWithAnswerConfiguredWhenCallingItWithOtherParametersThenTheExceptionContainsConfiguredAnswers() {
        val mock: DummyClass = mockk()
        val expectedMessage =
            "no answer found for $mock.function(3) [function(Int): Int] among the configured answers: ($mock.function(eq(2)) [function(Int): Int])"

        every {
            mock.function(2)
        } returns 3

        val exception =
            assertThrows<MockKException> {
                mock.function(3)
            }

        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun givenAMockkStubWithAnswersConfiguredWhenCallingItWithOtherParametersThenTheExceptionContainsConfiguredAnswers() {
        val mock: DummyClass = mockk()
        val expectedMessage =
            """no answer found for $mock.function(3) [function(Int): Int] among the configured answers: ($mock.function(eq(2)) [function(Int): Int]
$mock.function(eq(5)) [function(Int): Int])"""

        every {
            mock.function(2)
        } returns 3
        every {
            mock.function(5)
        } returns 3

        val exception =
            assertThrows<MockKException> {
                mock.function(3)
            }

        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun givenAMockkStubWithOverloadedMethodsWhenCallingTheUnstubbedOverloadThenTheExceptionContainsTheTypeInformation() {
        val mock: OverloadedClass = mockk()
        val expectedMessage =
            "no answer found for $mock.function(InputDataB) [function(InputData): InputData] " +
                "among the configured answers: ($mock.function(eq(InputDataA)) [function(InputDataA): InputDataA])"

        every {
            mock.function(InputDataA)
        } returns InputDataA

        val exception =
            assertThrows<MockKException> {
                mock.function(InputDataB)
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

    interface InputData

    object InputDataA : InputData {
        override fun toString(): String = "InputDataA"
    }

    object InputDataB : InputData {
        override fun toString(): String = "InputDataB"
    }

    class OverloadedClass {
        fun function(item: InputData): InputData = InputDataA

        fun function(a: InputDataA): InputDataA = InputDataA
    }
}
