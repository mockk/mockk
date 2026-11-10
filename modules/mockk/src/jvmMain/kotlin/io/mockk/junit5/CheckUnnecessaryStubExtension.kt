package io.mockk.junit5

import io.mockk.checkUnnecessaryStub
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * MockK JUnit 5 extension that runs the unnecessary stubbing check after each test method and
 * names the current test in the failure message.
 *
 * The `@MockKExtension.CheckUnnecessaryStub` annotation can only attribute the failure to the
 * whole test class when the check runs in an `@AfterAll` callback (e.g. with the `PER_CLASS`
 * lifecycle), so it is not always possible to tell which test declared the offending stub
 * ([#1344](https://github.com/mockk/mockk/issues/1344)). This extension is an opt-in alternative:
 * it calls [checkUnnecessaryStub] right after every test method, so JUnit reports the failure
 * against that test and the message contains the test's display name.
 *
 * [AfterTestExecutionCallback] is used instead of [org.junit.jupiter.api.extension.AfterEachCallback]
 * so that the check always runs before any `@AfterEach` method and before the cleanup performed by
 * [MockKExtension], no matter in which order the extensions are registered.
 *
 * Usage together with [MockKExtension]:
 *
 * ```
 * @ExtendWith(MockKExtension::class, CheckUnnecessaryStubExtension::class)
 * class CarTest {
 *   ...
 * }
 * ```
 *
 * This extension only performs the check: it does not initialize or clean up mocks, and it does not
 * change the class-level behavior of [MockKExtension]. Note that [checkUnnecessaryStub] inspects all
 * mocks registered at the moment of the call, so a stub left unused by an earlier test is reported
 * against the current test unless mocks are cleared between tests (as [MockKExtension] does by
 * default after each test).
 */
class CheckUnnecessaryStubExtension : AfterTestExecutionCallback {
    override fun afterTestExecution(context: ExtensionContext) {
        try {
            checkUnnecessaryStub()
        } catch (e: AssertionError) {
            throw AssertionError(
                "Test '${context.displayName}' has unnecessary stubbings.\n\n${e.message}",
                e,
            )
        }
    }
}
