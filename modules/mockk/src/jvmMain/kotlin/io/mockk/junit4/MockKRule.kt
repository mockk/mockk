package io.mockk.junit4

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A simple JUnit 4 rule which handles the setting up and tearing down of mock objects using the
 * [MockK] or [RelaxedMockK] annotations at the beginning and end of each test respectively -
 * without needing to manually call [unmockkAll] or [MockKAnnotations.init].
 *
 * Example:
 *
 * ```
 * class ExampleTest {
 *   @get:Rule
 *   val mockkRule = MockKRule(this)
 *
 *   @MockK
 *   private lateinit var car: Car
 *
 *   @Test
 *   fun something() {
 *      every { car.drive() } just runs
 *      ...
 *   }
 * }
 * ```
 *
 * Optionally, pass `checkUnnecessaryStub = true` to also run the unnecessary stubbing check after
 * every test, so a stub that was never used fails that test. The failure message names the current
 * test, and the check is skipped when the test itself failed, so a real failure is never masked by
 * a stubbing report. It is the JUnit 4 counterpart of the JUnit 5
 * [io.mockk.junit5.CheckUnnecessaryStubExtension]
 * ([#1344](https://github.com/mockk/mockk/issues/1344)) and performs the very check of the
 * standalone [CheckUnnecessaryStubRule], just without the need for a second rule. The check looks
 * at every mock registered at the moment of the call, so a stub left unused by an earlier test is
 * reported against the current test unless mocks are cleared between tests. This option is
 * disabled by default: the rule's behavior is unchanged unless it is switched on.
 *
 * ```
 * class CarTest {
 *   @get:Rule
 *   val mockkRule = MockKRule(this, checkUnnecessaryStub = true)
 *
 *   ...
 * }
 * ```
 */
class MockKRule(
    private val testSubject: Any,
    private val checkUnnecessaryStub: Boolean = false,
) : TestWatcher(),
    TestRule {
    override fun starting(description: Description?) {
        super.starting(description)
        MockKAnnotations.init(testSubject)
    }

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    starting(description)

                    val checkedBase =
                        if (checkUnnecessaryStub) {
                            CheckUnnecessaryStubRule().apply(base, description)
                        } else {
                            base
                        }
                    checkedBase.evaluate()
                } finally {
                    finished(description)
                }
            }
        }

    override fun finished(description: Description?) {
        super.finished(description)
        unmockkAll()
    }
}
