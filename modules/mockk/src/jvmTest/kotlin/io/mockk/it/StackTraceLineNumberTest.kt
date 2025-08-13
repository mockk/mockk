@file:Suppress("UNUSED_VALUE")

package io.mockk.it

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Test related to GitHub issue #102
 */
class StackTraceLineNumberTest {
    @Test
    fun everyLineNumberIsCorrect() {
        var stackTrace: Array<StackTraceElement>? = null
        val ex = try {
            stackTrace = Thread.currentThread().stackTrace; every { throw RuntimeException("failure") }
            fail("No exception thrown")
        } catch (ex: Exception) {
            ex
        }

        checkLineNumber(ex, stackTrace, "everyLineNumberIsCorrect")
    }

    @Suppress("UNUSED_VALUE")
    @Test
    fun verifyLineNumberIsCorrect() {
        var stackTrace: Array<StackTraceElement>? = null
        val ex = try {
            stackTrace = Thread.currentThread().stackTrace; verify { throw RuntimeException("failure") }
            fail("No exception thrown")
        } catch (ex: Exception) {
            ex
        }

        checkLineNumber(ex, stackTrace, "verifyLineNumberIsCorrect")
    }

    private fun Array<StackTraceElement>.lineNumber(test: String, methodName: String): Int {
        return first {
            it.className == test &&
                    it.methodName == methodName
        }.lineNumber
    }


    private fun checkLineNumber(
        ex: Exception,
        stackTrace: Array<StackTraceElement>?,
        methodName: String
    ) {
        val actualLN = ex.stackTrace.lineNumber(
            "io.mockk.it.StackTraceLineNumberTest",
            methodName
        )

        val expectedLN = stackTrace!!.lineNumber(
            "io.mockk.it.StackTraceLineNumberTest",
            methodName
        )

        assertEquals(expectedLN, actualLN)
    }
}
