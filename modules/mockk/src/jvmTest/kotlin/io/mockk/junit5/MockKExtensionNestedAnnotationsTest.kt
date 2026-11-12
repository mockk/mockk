package io.mockk.junit5

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.util.assertGarbageCollected
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod
import org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedMethod
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.testkit.engine.EngineExecutionResults
import org.junit.platform.testkit.engine.EngineTestKit
import java.lang.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Checks that [MockKExtension] class annotations on an outer test class also apply to its `@Nested` classes.
 *
 * The fixtures below are meant to fail or to leave mocks behind, so they are `@Disabled` for the normal test run
 * and only run through [EngineTestKit], which turns that condition off.
 */
class MockKExtensionNestedAnnotationsTest {
    @AfterEach
    fun resetMocks() {
        unmockkObject(Target)
        // Fixtures with KeepMocks leave their mocks' stubs and calls behind.
        clearAllMocks()
    }

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

            assertSucceeded(result)
        }

        @Test
        fun `passes the README example, where only the top-level test verifies the outer BeforeAll call`() {
            val results = runTests(selectClass(BeforeAllCallFixture::class.java))

            assertEquals(2, results.size, "Expected startsEngine() and Drive.drives() to run")
            results.forEach { assertSucceeded(it) }
        }

        @Test
        fun `passes a nested test that does not verify a call made in the outer BeforeAll`() {
            // Run alone, so no top-level test runs first and clears the call.
            val result = runNestedTest<BeforeAllCallFixture, BeforeAllCallFixture.Drive>("drives")

            assertSucceeded(result)
        }

        @Test
        fun `fails a top-level test that does not verify a call made in the BeforeAll`() {
            val result = runTest(selectMethod(BeforeAllCallUnverifiedFixture::class.java, "drives"))

            assertFailedWith(VERIFICATION_FAILED, result)
        }

        @Test
        fun `fails a PER_CLASS outer class with KeepMocks whose test leaves a call unverified, when it has a nested class`() {
            // The outer class checks its calls only in afterAll, after its nested class has run.
            val result = runClass(KeepMocksPerClassFixture::class.java)

            assertFailedWith(VERIFICATION_FAILED, result)
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

            assertSucceeded(result)
        }
    }

    @Nested
    inner class WithKeepMocks {
        @Test
        fun `keeps mocks after a nested test when the outer class is annotated`() {
            val result = runNestedTest<KeepMocksFixture, KeepMocksFixture.Inner>("stubsTarget")

            assertSucceeded(result)
            assertEquals(STUBBED, Target.call(), "Expected the stub to survive the nested test")
        }

        @Test
        fun `keeps mocks after a nested test when the nested class is annotated`() {
            val result = runNestedTest<UnannotatedFixture, UnannotatedFixture.AnnotatedInner>("stubsTarget")

            assertSucceeded(result)
            assertEquals(STUBBED, Target.call(), "Expected the stub to survive the nested test")
        }

        @Test
        fun `resets mocks after a nested test when no class is annotated`() {
            val result = runNestedTest<UnannotatedFixture, UnannotatedFixture.Inner>("stubsTarget")

            assertSucceeded(result)
            assertEquals(REAL, Target.call(), "Expected the stub to be removed after the nested test")
        }

        @Test
        fun `resets mocks after a test in a static nested class of an annotated class`() {
            val result = runTest(selectClass(KeepMocksFixture.StaticNested::class.java))

            assertSucceeded(result)
            assertEquals(REAL, Target.call(), "Expected a static nested class to ignore its outer class's annotations")
        }
    }

    @Nested
    inner class WithRequireParallelTesting {
        @Test
        fun `does not reset mocks after a nested test`() {
            val result = runNestedTest<RequireParallelTestingFixture, RequireParallelTestingFixture.Inner>("stubsTarget")

            assertSucceeded(result)
            assertEquals(STUBBED, Target.call(), "Expected the stub to survive the nested test")
        }
    }

    @Nested
    inner class WithReleaseMocks {
        @Test
        fun `releases mocks after a nested test`() {
            val result = runNestedTest<ReleaseMocksFixture, ReleaseMocksFixture.Inner>("createsMocks")

            assertSucceeded(result)
            val refs = createdMocks.toList()
            createdMocks.clear()
            assertGarbageCollected(refs, "Expected mocks created in the nested test to be released")
        }

        @Test
        fun `keeps the mocks of a PER_CLASS outer class usable in every nested test`() {
            val results = runTests(selectClass(ReleaseMocksPerClassFixture::class.java))

            assertEquals(2, results.size, "Expected both nested tests to run")
            results.forEach { assertSucceeded(it) }
        }

        @Test
        fun `releases the mocks of a PER_CLASS outer class when the outer class finishes`() {
            runTests(selectClass(ReleaseMocksPerClassFixture::class.java))

            val refs = createdMocks.toList()
            createdMocks.clear()
            assertGarbageCollected(refs, "Expected the outer class's mock to be released")
        }
    }

    @Nested
    inner class WithUseDependencyOrder {
        @Test
        fun `injects mocks in dependency order in a nested class`() {
            val result = runNestedTest<UseDependencyOrderFixture, UseDependencyOrderFixture.Inner>("injectsDependencies")

            assertSucceeded(result)
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

    /** The `CarTest` example from the README's "Nested test classes" section. */
    @Disabled(FIXTURE)
    @MockKExtension.ConfirmVerification
    @ExtendWith(MockKExtension::class)
    class BeforeAllCallFixture {
        companion object {
            val engine = mockk<Engine>(relaxed = true)

            @JvmStatic
            @BeforeAll
            fun warmUp() {
                engine.start()
            }
        }

        @Test
        fun startsEngine() {
            verify { engine.start() }
        }

        @Nested
        inner class Drive {
            @Test
            fun drives() {
                // Does not verify engine.start()
            }
        }
    }

    @Disabled(FIXTURE)
    @MockKExtension.ConfirmVerification
    @ExtendWith(MockKExtension::class)
    class BeforeAllCallUnverifiedFixture {
        companion object {
            val engine = mockk<Engine>(relaxed = true)

            @JvmStatic
            @BeforeAll
            fun warmUp() {
                engine.start()
            }
        }

        @Test
        fun drives() {
            // Does not verify engine.start()
        }
    }

    @Disabled(FIXTURE)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @MockKExtension.KeepMocks
    @MockKExtension.ConfirmVerification
    @ExtendWith(MockKExtension::class)
    class KeepMocksPerClassFixture {
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
            fun doesNothing() {
                // Only here so that a nested class starts before the outer class checks its calls
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

    @Disabled(FIXTURE)
    @MockKExtension.KeepMocks
    @ExtendWith(MockKExtension::class)
    class KeepMocksFixture {
        @Nested
        inner class Inner {
            @Test
            fun stubsTarget() = stubTarget()
        }

        // Not @Nested: JUnit runs it as a standalone class, so the outer class's annotations must not apply.
        @Disabled(FIXTURE)
        @ExtendWith(MockKExtension::class)
        class StaticNested {
            @Test
            fun stubsTarget() = stubTarget()
        }
    }

    @Disabled(FIXTURE)
    @ExtendWith(MockKExtension::class)
    class UnannotatedFixture {
        @Nested
        inner class Inner {
            @Test
            fun stubsTarget() = stubTarget()
        }

        @Nested
        @MockKExtension.KeepMocks
        inner class AnnotatedInner {
            @Test
            fun stubsTarget() = stubTarget()
        }
    }

    @Disabled(FIXTURE)
    @MockKExtension.RequireParallelTesting
    @ExtendWith(MockKExtension::class)
    class RequireParallelTestingFixture {
        @Nested
        inner class Inner {
            @Test
            fun stubsTarget() = stubTarget()
        }
    }

    @Disabled(FIXTURE)
    @MockKExtension.ReleaseMocks
    @ExtendWith(MockKExtension::class)
    class ReleaseMocksFixture {
        @Nested
        inner class Inner {
            @Test
            fun createsMocks() {
                repeat(MOCK_COUNT) {
                    val mock = mockk<Runnable>()
                    every { mock.run() } just Runs
                    mock.run()
                    createdMocks += WeakReference(mock)
                }
            }
        }
    }

    @Disabled(FIXTURE)
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @MockKExtension.ReleaseMocks
    @ExtendWith(MockKExtension::class)
    class ReleaseMocksPerClassFixture {
        // Lives as long as the outer instance, so it must survive each nested test.
        @MockK
        lateinit var mock: Runnable

        @BeforeAll
        fun trackMock() {
            createdMocks += WeakReference(mock)
        }

        @Nested
        inner class Inner {
            @Test
            fun usesMock() = runMock()

            @Test
            fun usesMockAgain() = runMock()

            private fun runMock() {
                every { mock.run() } just Runs

                mock.run()
            }
        }
    }

    @Disabled(FIXTURE)
    @MockKExtension.UseDependencyOrder
    @ExtendWith(MockKExtension::class)
    class UseDependencyOrderFixture {
        @Nested
        inner class Inner {
            @MockK
            lateinit var repository: Repository

            // Declared before the service it depends on, so injection only works in dependency order.
            @InjectMockKs
            lateinit var controller: Controller

            @InjectMockKs
            lateinit var service: Service

            @Test
            fun injectsDependencies() {
                assertSame(service, controller.service)
            }
        }
    }

    object Target {
        fun call(): String = REAL
    }

    interface Engine {
        fun start()
    }

    interface Repository

    class Service(
        val repository: Repository,
    )

    class Controller(
        val service: Service,
    )

    private companion object {
        const val FIXTURE = "Fixture: runs only through EngineTestKit"
        const val VERIFICATION_FAILED = "Verification acknowledgment failed"
        const val UNNECESSARY_STUB = "Unnecessary stubbings detected"
        const val REAL = "real"
        const val STUBBED = "stubbed"
        const val MOCK_COUNT = 100

        /** Mocks created by the ReleaseMocks fixtures, held weakly so only MockK can keep them alive. */
        val createdMocks = mutableListOf<WeakReference<Runnable>>()

        fun stubTarget() {
            mockkObject(Target)
            every { Target.call() } returns STUBBED
        }

        inline fun <reified Outer, reified Inner> runNestedTest(methodName: String): TestExecutionResult =
            runTest(selectNestedMethod(listOf(Outer::class.java), Inner::class.java, methodName))

        /** Runs the single selected fixture test, with `@Disabled` turned off, and returns its result. */
        fun runTest(selector: DiscoverySelector): TestExecutionResult = runTests(selector).single()

        /** Runs the selected fixture tests, with `@Disabled` turned off, and returns their results in run order. */
        fun runTests(selector: DiscoverySelector): List<TestExecutionResult> =
            execute(selector)
                .testEvents()
                .finished()
                .list()
                .map { it.getRequiredPayload(TestExecutionResult::class.java) }

        /**
         * Runs every test in [fixture], with `@Disabled` turned off, and returns the result of the class itself.
         * It fails when the class's own callbacks fail, like a check in `afterAll`.
         */
        fun runClass(fixture: Class<*>): TestExecutionResult =
            execute(selectClass(fixture))
                .containerEvents()
                .finished()
                .list()
                .single { (it.testDescriptor.source.orElse(null) as? ClassSource)?.className == fixture.name }
                .getRequiredPayload(TestExecutionResult::class.java)

        private fun execute(selector: DiscoverySelector): EngineExecutionResults =
            EngineTestKit
                .engine("junit-jupiter")
                .configurationParameter("junit.jupiter.conditions.deactivate", "org.junit.*DisabledCondition")
                .selectors(selector)
                .execute()

        fun assertSucceeded(result: TestExecutionResult) {
            assertEquals(
                TestExecutionResult.Status.SUCCESSFUL,
                result.status,
                "Expected the fixture test to pass, but it failed with: ${result.throwable.orElse(null)}",
            )
        }

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
