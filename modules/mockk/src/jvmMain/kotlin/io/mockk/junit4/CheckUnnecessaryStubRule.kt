package io.mockk.junit4

import io.mockk.checkUnnecessaryStub
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * MockK JUnit 4 rule that runs the unnecessary stubbing check after each test and names the
 * current test in the failure message. It is the JUnit 4 counterpart of the JUnit 5
 * [io.mockk.junit5.CheckUnnecessaryStubExtension]
 * ([#1344](https://github.com/mockk/mockk/issues/1344)).
 *
 * The check runs right after the test body, before `@After` methods and before the
 * [MockKRule] teardown, so the failure is reported against that test and its message contains
 * the test's display name. If the test itself failed, its own failure is reported and the
 * stubbing check is skipped, so a real failure is never masked by a stubbing report.
 *
 * Usage together with [MockKRule]:
 *
 * ```
 * class CarTest {
 *   @get:Rule
 *   val mockkRule = MockKRule(this)
 *
 *   @get:Rule
 *   val checkUnnecessaryStubRule = CheckUnnecessaryStubRule()
 *
 *   ...
 * }
 * ```
 *
 * This rule only performs the check: it does not initialize or clean up mocks, and it does not
 * change the behavior of [MockKRule]. Note that [checkUnnecessaryStub] inspects all mocks
 * registered at the moment of the call, so a stub left unused by an earlier test is reported
 * against the current test unless mocks are cleared between tests.
 */
class CheckUnnecessaryStubRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                // If the test itself failed, its own exception propagates and the check is
                // skipped: a stubbing report must not mask the actual failure.
                base.evaluate()

                try {
                    checkUnnecessaryStub()
                } catch (e: AssertionError) {
                    throw AssertionError(
                        "Test '${description.displayName}' has unnecessary stubbings.\n\n${e.message}",
                        e,
                    )
                }
            }
        }
}
