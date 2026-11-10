package io.mockk.junit5

import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class CheckUnnecessaryStubExtensionTest {
    @Test
    fun `unnecessary stubbing is reported against the test that declared it`() {
        val results = runTestMethods(CheckUnnecessaryStubAttribution::class.java)

        val usedStub = results.byMethodName("usedStub")
        assertEquals(
            TestExecutionResult.Status.SUCCESSFUL,
            usedStub.status,
            "the test that uses its stub must be unaffected: ${usedStub.throwable.orElse(null)}",
        )

        val unusedStub = results.byMethodName("unusedStub")
        assertEquals(TestExecutionResult.Status.FAILED, unusedStub.status)
        val error = assertIs<AssertionError>(unusedStub.throwable.orElse(null))
        assertTrue(error.message!!.contains("unusedStub"), "actual message: ${error.message}")
        assertTrue(
            error.message!!.contains("Unnecessary stubbings detected"),
            "actual message: ${error.message}",
        )
    }

    /**
     * Runs [testClass] through the JUnit Platform launcher and returns the result of each test
     * method, so that the extension failure can be checked against the test it was reported for.
     */
    private fun runTestMethods(testClass: Class<*>): Map<String, TestExecutionResult> {
        val results = mutableMapOf<String, TestExecutionResult>()
        val request =
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(selectClass(testClass))
                .build()

        LauncherFactory.create().execute(
            request,
            object : TestExecutionListener {
                override fun executionFinished(
                    testIdentifier: TestIdentifier,
                    testExecutionResult: TestExecutionResult,
                ) {
                    if (testIdentifier.isTest) {
                        results[testIdentifier.displayName] = testExecutionResult
                    }
                }
            },
        )

        return results
    }

    private fun Map<String, TestExecutionResult>.byMethodName(name: String): TestExecutionResult =
        filterKeys { it.startsWith(name) }
            .values
            .singleOrNull()
            ?: fail("expected exactly one test method named '$name', got: $keys")
}

/**
 * Declares one used and one unused stub. The display name of the test that declares the unused
 * stub must appear in the reported failure, while the other test stays green.
 *
 * The class name intentionally does not end in "Test" so the Gradle test discovery does not run
 * it directly: it is executed above through the JUnit Platform launcher.
 */
@ExtendWith(MockKExtension::class, CheckUnnecessaryStubExtension::class)
class CheckUnnecessaryStubAttribution {
    interface Car {
        fun drive(): String
    }

    @MockK
    lateinit var car: Car

    @Test
    fun usedStub() {
        every { car.drive() } returns "ok"

        assertEquals("ok", car.drive())
    }

    @Test
    fun unusedStub() {
        every { car.drive() } returns "ok"
    }
}
