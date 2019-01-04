package io.mockk.gh

import io.mockk.every
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class Issue102Test {
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
            "io.mockk.gh.Issue102Test",
            methodName
        )

        val expectedLN = stackTrace!!.lineNumber(
            "io.mockk.gh.Issue102Test",
            methodName
        )

        assertEquals(expectedLN, actualLN)
    }
}
