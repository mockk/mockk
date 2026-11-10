package io.mockk.junit4

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Declares one used and one unused stub. The name of the test that declares the unused stub must
 * appear in the reported failure, while the other test stays green.
 *
 * [clearAllMocks] in the setup resets the answers left over by previous tests: unlike
 * `MockKExtension` (JUnit 5), the [MockKRule] teardown does not clear them, and the unnecessary
 * stubbing check inspects all mocks registered at the moment of the call.
 *
 * The class name intentionally does not end in "Test" so the Gradle test discovery does not run
 * it directly: it is executed by [CheckUnnecessaryStubRuleTest] through [org.junit.runner.JUnitCore].
 */
class CheckUnnecessaryStubAttribution {
    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val checkUnnecessaryStubRule = CheckUnnecessaryStubRule()

    interface Car {
        fun drive(): String
    }

    @MockK
    lateinit var car: Car

    @Before
    fun resetStubs() {
        clearAllMocks()
    }

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
