package io.mockk.it

import io.mockk.*
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepeatedlyAnswersTest {
    private val mock = mockk<MockClass>()

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun repeatedlyAnswers(times: Int) {
        every { mock.op() } repeatedly times answers ConstantAnswer("repeating") andThen "final"

        repeat(times) {
            assertEquals("repeating", mock.op())
        }
        assertEquals("final", mock.op())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun repeatedlyAnswersLambda(times: Int) {
        every { mock.op() } repeatedly times answers { "repeating" } andThen "final"

        repeat(times) {
            assertEquals("repeating", mock.op())
        }
        assertEquals("final", mock.op())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun repeatedlyReturns(times: Int) {
        every { mock.op() } repeatedly times returns "repeating" andThen "final"

        repeat(times) {
            assertEquals("repeating", mock.op())
        }
        assertEquals("final", mock.op())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun repeatedlyThrows(times: Int) {
        every { mock.op() } repeatedly times throws  RuntimeException("repeating") andThen "final"

        repeat(times) {
            assertFailsWith(RuntimeException::class, message = "repeating") {
                mock.op()
            }
        }
        assertEquals("final", mock.op())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun repeatedlyCoAnswers(times: Int) {
        every { mock.op() } repeatedly times coAnswers { coroutineScope { "repeating" } } andThen "final"

        repeat(times) {
            assertEquals("repeating", mock.op())
        }
        assertEquals("final", mock.op())
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 2, 3])
    fun repeatedlyJustRuns(times: Int) {
        every { mock.unitOp() } repeatedly times just Runs andThenThrows RuntimeException("final")

        repeat(times) {
            assertDoesNotThrow {
                mock.unitOp()
            }
        }
        assertFailsWith(RuntimeException::class, message = "final") {
            mock.unitOp()
        }
    }

    private interface MockClass {
        fun op(): String
        fun unitOp()
    }
}
