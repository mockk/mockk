package io.mockk.junit4

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * JUnit 4 scenarios for the opt-in `checkUnnecessaryStub` option of [MockKRule], executed by
 * [MockKRuleCheckUnnecessaryStubTest] through [org.junit.runner.JUnitCore]. The class names
 * intentionally do not end in "Test" so the Gradle test discovery does not run them directly:
 * their designed failures would break the suite.
 *
 * Each class declares one interface mock and either uses its stub, leaves it unused, or fails
 * on purpose, so the driver can pin how the check is reported.
 */
interface MockKRuleCheckUnnecessaryStubCar {
    fun drive(): String
}

/**
 * Enables the check and declares one used and one unused stub. The name of the test that
 * declares the unused stub must appear in the reported failure, while the other test stays
 * green.
 *
 * [clearAllMocks] in the setup resets the answers left over by previous tests: unlike
 * `MockKExtension` (JUnit 5), the JUnit 4 teardown does not clear them, and the unnecessary
 * stubbing check inspects all mocks registered at the moment of the call.
 */
class MockKRuleCheckUnnecessaryStubAttribution {
    @get:Rule
    val mockkRule = MockKRule(this, checkUnnecessaryStub = true)

    @MockK
    lateinit var car: MockKRuleCheckUnnecessaryStubCar

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

/**
 * Leaves the check disabled (the default): an unused stub must not fail anything, matching the
 * pre-existing rule behavior.
 */
class MockKRuleCheckUnnecessaryStubDisabledAttribution {
    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    lateinit var car: MockKRuleCheckUnnecessaryStubCar

    @Test
    fun unusedStub() {
        every { car.drive() } returns "ok"
    }
}

/**
 * Enables the check and uses every stub it declares: nothing must be reported and the test
 * stays green.
 */
class MockKRuleCheckUnnecessaryStubCleanAttribution {
    @get:Rule
    val mockkRule = MockKRule(this, checkUnnecessaryStub = true)

    @MockK
    lateinit var car: MockKRuleCheckUnnecessaryStubCar

    @Test
    fun usedStub() {
        every { car.drive() } returns "ok"

        assertEquals("ok", car.drive())
    }
}

/**
 * Enables the check but fails the test itself after declaring an unused stub. The test's own
 * failure must be reported and the stubbing check skipped, so a real failure is never masked.
 */
class MockKRuleCheckUnnecessaryStubFailedTestAttribution {
    @get:Rule
    val mockkRule = MockKRule(this, checkUnnecessaryStub = true)

    @MockK
    lateinit var car: MockKRuleCheckUnnecessaryStubCar

    @Test
    fun failingTest() {
        every { car.drive() } returns "ok"

        throw IllegalStateException("the real failure")
    }
}
