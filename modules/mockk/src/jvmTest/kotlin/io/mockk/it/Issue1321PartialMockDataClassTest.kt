package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for issue #1321: when a method is partially mocked with
 * `callOriginal()`, its body reads properties through their backing fields,
 * so a property getter stubbed with a constant answer must be reflected in
 * the backing field for the original method body to observe the stubbed
 * value.
 *
 * Kotlin compiles properties whose name starts with "is" (e.g. `isActive`)
 * to a getter that keeps the property name instead of prefixing it with
 * `get`, which previously made the constant answer write look for a field
 * that did not match and silently skip it.
 */
class Issue1321PartialMockDataClassTest {
    data class UIDevice(
        val isActive: Boolean?,
    ) {
        fun isOnline(): Boolean = isActive != null && isActive == true
    }

    private fun partiallyMockedDevice() =
        mockk<UIDevice> {
            every { isOnline() } answers { callOriginal() }
        }

    /** https://github.com/mockk/mockk/issues/1321 */
    @Test
    fun `original method observes getter stubbed with true`() {
        val device = partiallyMockedDevice()
        every { device.isActive } returns true
        assertEquals(true, device.isOnline())
    }

    @Test
    fun `original method observes getter stubbed with false`() {
        val device = partiallyMockedDevice()
        every { device.isActive } returns false
        assertEquals(false, device.isOnline())
    }

    @Test
    fun `original method observes getter stubbed with null`() {
        val device = partiallyMockedDevice()
        every { device.isActive } returns null
        assertEquals(false, device.isOnline())
    }

    /**
     * The data class in the issue is not what breaks partial mocking: any class
     * with an "is"-prefixed property behaves the same.
     */
    class TrafficLight(
        val isActive: Boolean?,
        var isBlocked: Boolean?,
    ) {
        fun isActiveNow(): Boolean = isActive != null && isActive == true

        fun isBlockedNow(): Boolean = isBlocked != null && isBlocked == true
    }

    private fun partiallyMockedTrafficLight() =
        mockk<TrafficLight> {
            every { isActiveNow() } answers { callOriginal() }
            every { isBlockedNow() } answers { callOriginal() }
        }

    @Test
    fun `original method observes is-prefixed val getter stub on plain class`() {
        val light = partiallyMockedTrafficLight()
        every { light.isActive } returns true
        assertEquals(true, light.isActiveNow())
    }

    @Test
    fun `original method observes is-prefixed var getter stub on plain class`() {
        val light = partiallyMockedTrafficLight()
        every { light.isBlocked } returns true
        assertEquals(true, light.isBlockedNow())
    }

    /**
     * For an abstract class the proxy is a subclass instead of the retransformed
     * class itself; property reads during `callOriginal()` are then routed through
     * the intercepted getter, so this path must keep observing stubbed values.
     */
    abstract class AbstractDevice {
        open val isActive: Boolean? = null

        open fun isOnline(): Boolean = isActive != null && isActive == true
    }

    @Test
    fun `original method observes getter stub on subclass proxy`() {
        val device =
            mockk<AbstractDevice> {
                every { isOnline() } answers { callOriginal() }
            }
        every { device.isActive } returns true
        assertEquals(true, device.isOnline())
    }
}
