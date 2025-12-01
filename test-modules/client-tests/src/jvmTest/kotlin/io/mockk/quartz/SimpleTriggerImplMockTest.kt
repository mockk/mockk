package io.mockk.quartz

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Test that reproduces GitHub issue #1432:
 * MockK 1.14.6 can no longer mock Quartz SimpleTriggerImpl
 *
 * The issue is that the Trigger interface extends Cloneable, whereas the actual clone()
 * method is only specified in a subinterface (MutableTrigger). An implementation that
 * inherits from both cannot be mocked due to a "Cannot infer visibility" error.
 *
 * This error is caused by Kotlin reflection (method.kotlinFunction) throwing an
 * IllegalStateException when analyzing the inheritance hierarchy for the clone() method.
 */
class SimpleTriggerImplMockTest {

    @Test
    fun `should be able to mock SimpleTriggerImpl with complex inheritance hierarchy`() {
        val trigger = mockk<SimpleTriggerImpl> {
            every { timesTriggered } returns 1
        }

        assertEquals(1, trigger.timesTriggered)
    }

    @Test
    fun `should be able to mock SimpleTrigger interface`() {
        val trigger = mockk<SimpleTrigger> {
            every { timesTriggered } returns 42
        }

        assertEquals(42, trigger.timesTriggered)
    }

    @Test
    fun `should be able to mock MutableTrigger interface`() {
        val trigger = mockk<MutableTrigger> {
            every { name } returns "test"
        }

        assertEquals("test", trigger.name)
    }

    @Test
    fun `should be able to access clone method on mocked SimpleTriggerImpl`() {
        val trigger = mockk<SimpleTriggerImpl>(relaxed = true)

        // Just verify that calling methods on the mock doesn't throw an exception
        trigger.clone()
        trigger.name
        trigger.timesTriggered
    }
}
