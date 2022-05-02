package io.mockk.junit4

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import org.junit.rules.TestRule
import org.junit.rules.TestWatcher
import org.junit.runner.Description

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
 */
class MockKRule(private val testSubject: Any) : TestWatcher(), TestRule {
    override fun starting(description: Description?) {
        super.starting(description)
        MockKAnnotations.init(testSubject)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        unmockkAll()
    }
}
