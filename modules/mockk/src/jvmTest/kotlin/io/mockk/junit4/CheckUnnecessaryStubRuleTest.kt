package io.mockk.junit4

import io.mockk.clearAllMocks
import org.junit.jupiter.api.Test
import org.junit.runner.JUnitCore
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CheckUnnecessaryStubRuleTest {
    @Test
    fun `unnecessary stubbing is reported against the test that declared it`() {
        val result = JUnitCore.runClasses(CheckUnnecessaryStubAttribution::class.java)

        assertEquals(1, result.failureCount, "failures: ${result.failures.map { it.description }}")
        val failure = result.failures.single()
        assertEquals("unusedStub", failure.description.methodName)

        val error = assertIs<AssertionError>(failure.exception)
        assertTrue(error.message!!.contains("unusedStub"), "actual message: ${error.message}")
        assertTrue(
            error.message!!.contains("Unnecessary stubbings detected"),
            "actual message: ${error.message}",
        )

        // the JUnit 4 scenario does not clear stubs between tests, reset the global state
        clearAllMocks()
    }
}
