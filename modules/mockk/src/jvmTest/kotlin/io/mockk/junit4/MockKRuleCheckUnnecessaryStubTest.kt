package io.mockk.junit4

import io.mockk.clearAllMocks
import org.junit.jupiter.api.Test
import org.junit.runner.JUnitCore
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MockKRuleCheckUnnecessaryStubTest {
    @Test
    fun `unnecessary stubbing is reported against the test that declared it`() {
        val result = JUnitCore.runClasses(MockKRuleCheckUnnecessaryStubAttribution::class.java)

        assertEquals(2, result.runCount, "both tests must have run")
        assertEquals(1, result.failureCount, "failures: ${result.failures.map { it.description }}")

        val failure = result.failures.single()
        assertEquals("unusedStub", failure.description.methodName)

        val error = assertIs<AssertionError>(failure.exception)
        assertTrue(error.message!!.contains("unusedStub"), "actual message: ${error.message}")
        assertTrue(error.message!!.contains("has unnecessary stubbings"), "actual message: ${error.message}")
        assertTrue(
            error.message!!.contains("Unnecessary stubbings detected"),
            "actual message: ${error.message}",
        )

        // the JUnit 4 scenarios do not clear stubs between tests, reset the global state
        clearAllMocks()
    }

    @Test
    fun `unnecessary stubbing is not checked when the option is off`() {
        val result = JUnitCore.runClasses(MockKRuleCheckUnnecessaryStubDisabledAttribution::class.java)

        assertEquals(0, result.failureCount, "failures: ${result.failures.map { it.description }}")

        clearAllMocks()
    }

    @Test
    fun `a used stub passes when the option is on`() {
        val result = JUnitCore.runClasses(MockKRuleCheckUnnecessaryStubCleanAttribution::class.java)

        assertEquals(0, result.failureCount, "failures: ${result.failures.map { it.description }}")

        clearAllMocks()
    }

    @Test
    fun `a failing test reports its own failure instead of the stubbing check`() {
        val result = JUnitCore.runClasses(MockKRuleCheckUnnecessaryStubFailedTestAttribution::class.java)

        assertEquals(1, result.failureCount, "failures: ${result.failures.map { it.description }}")

        val failure = result.failures.single()
        assertEquals("failingTest", failure.description.methodName)

        val error = assertIs<IllegalStateException>(failure.exception)
        assertTrue(error.message!!.contains("the real failure"), "actual message: ${error.message}")

        clearAllMocks()
    }
}
