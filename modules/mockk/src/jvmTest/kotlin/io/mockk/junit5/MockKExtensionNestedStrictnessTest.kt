package io.mockk.junit5

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod
import org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedMethod
import org.junit.platform.testkit.engine.EngineTestKit
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Checks that strictness annotations on an outer test class also apply to its `@Nested` classes.
 *
 * The fixtures below are meant to fail, so they are `@Disabled` for the normal test run
 * and only run through [EngineTestKit], which turns that condition off.
 */
class MockKExtensionNestedStrictnessTest {
    @Nested
    inner class WithConfirmVerification {
        @Test
        fun `fails a top-level test that leaves a call unverified`() {
            val result = runTest(selectMethod(ConfirmVerificationFixture::class.java, "leavesCallUnverified"))

            assertFailedWith(VERIFICATION_FAILED, result)
        }

        @Test
        fun `fails a nested test that leaves a call unverified`() {
            val result = runNestedTest<ConfirmVerificationFixture, ConfirmVerificationFixture.Inner>("leavesCallUnverified")

            assertFailedWith(VERIFICATION_FAILED, result)
        }

        @Test
        fun `passes a nested test that verifies its calls`() {
            val result = runNestedTest<ConfirmVerificationFixture, ConfirmVerificationFixture.Inner>("verifiesCall")

            assertEquals(TestExecutionResult.Status.SUCCESSFUL, result.status)
        }
    }

    @Nested
    inner class WithCheckUnnecessaryStub {
        @Test
        fun `fails a top-level test that leaves a stub unused`() {
            val result = runTest(selectMethod(CheckUnnecessaryStubFixture::class.java, "leavesStubUnused"))

            assertFailedWith(UNNECESSARY_STUB, result)
        }

        @Test
        fun `fails a nested test that leaves a stub unused`() {
            val result = runNestedTest<CheckUnnecessaryStubFixture, CheckUnnecessaryStubFixture.Inner>("leavesStubUnused")

            assertFailedWith(UNNECESSARY_STUB, result)
        }

        @Test
        fun `passes a nested test that uses its stubs`() {
            val result = runNestedTest<CheckUnnecessaryStubFixture, CheckUnnecessaryStubFixture.Inner>("usesStub")

            assertEquals(TestExecutionResult.Status.SUCCESSFUL, result.status)
        }
    }

    @Disabled(FIXTURE)
    @MockKExtension.ConfirmVerification
    @ExtendWith(MockKExtension::class)
    class ConfirmVerificationFixture {
        @MockK
        lateinit var mock: Runnable

        @Test
        fun leavesCallUnverified() {
            every { mock.run() } just Runs

            mock.run()
        }

        @Nested
        inner class Inner {
            @Test
            fun leavesCallUnverified() {
                every { mock.run() } just Runs

                mock.run()
            }

            @Test
            fun verifiesCall() {
                every { mock.run() } just Runs

                mock.run()

                verify { mock.run() }
            }
        }
    }

    @Disabled(FIXTURE)
    @MockKExtension.CheckUnnecessaryStub
    @ExtendWith(MockKExtension::class)
    class CheckUnnecessaryStubFixture {
        @MockK
        lateinit var mock: Runnable

        @Test
        fun leavesStubUnused() {
            every { mock.run() } just Runs
        }

        @Nested
        inner class Inner {
            @Test
            fun leavesStubUnused() {
                every { mock.run() } just Runs
            }

            @Test
            fun usesStub() {
                every { mock.run() } just Runs

                mock.run()
            }
        }
    }

    private companion object {
        const val FIXTURE = "Fixture: runs only through EngineTestKit"
        const val VERIFICATION_FAILED = "Verification acknowledgment failed"
        const val UNNECESSARY_STUB = "Unnecessary stubbings detected"

        inline fun <reified Outer, reified Inner> runNestedTest(methodName: String): TestExecutionResult =
            runTest(selectNestedMethod(listOf(Outer::class.java), Inner::class.java, methodName))

        /** Runs the single selected fixture test, with `@Disabled` turned off, and returns its result. */
        fun runTest(selector: DiscoverySelector): TestExecutionResult =
            EngineTestKit
                .engine("junit-jupiter")
                .configurationParameter("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")
                .selectors(selector)
                .execute()
                .testEvents()
                .finished()
                .list()
                .single()
                .getRequiredPayload(TestExecutionResult::class.java)

        fun assertFailedWith(
            expectedMessageStart: String,
            result: TestExecutionResult,
        ) {
            assertEquals(TestExecutionResult.Status.FAILED, result.status, "Expected the fixture test to fail")
            val error = assertIs<AssertionError>(result.throwable.get())
            assertTrue(
                error.message.orEmpty().startsWith(expectedMessageStart),
                "Expected message starting with \"$expectedMessageStart\" but was: ${error.message}",
            )
        }
    }
}
