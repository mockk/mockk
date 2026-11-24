package io.mockk.it

import io.mockk.mockk
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests related to GitHub issue #1220: a relaxed mock used to hand back a child mock for
 * `kotlinx.datetime.Instant`. On kotlinx-datetime 0.5.x-0.6.x that type is a final class
 * wrapping a `java.time.Instant` behind an internal constructor, so the child mock carries a
 * null internal value and downstream java.time code failed with ClassCastExceptions.
 */
class KotlinxDatetimeInstantReturnOnRelaxedMockTest {
    interface InstantReturner {
        val instant: Instant

        fun now(): Instant
    }

    open class InstantHolder {
        val instant: Instant get() = Instant.fromEpochMilliseconds(42)
    }

    interface PlainValueReturner {
        val plain: PlainValue

        fun providePlain(): PlainValue
    }

    interface DurationReturner {
        val duration: kotlin.time.Duration
    }

    @JvmInline
    value class PlainValue(
        val id: Int,
    )

    @Test
    fun `relaxed interface property returns a real epoch instant`() {
        val mock = mockk<InstantReturner>(relaxed = true)

        assertRealEpochInstant(mock.instant)
    }

    @Test
    fun `relaxed interface function returns a real epoch instant`() {
        val mock = mockk<InstantReturner>(relaxed = true)

        assertRealEpochInstant(mock.now())
    }

    @Test
    fun `relaxed class property returns a real epoch instant`() {
        val mock = mockk<InstantHolder>(relaxed = true)

        assertRealEpochInstant(mock.instant)
    }

    /**
     * Reproduces the downstream failure from the issue (Exposed passing the mocked instant into
     * real java.time machinery threw `ClassCastException: class java.lang.Object cannot be cast
     * to class java.time.chrono.Chronology`): the returned value must survive real formatting.
     */
    @Test
    fun `relaxed instant survives real java-time formatting`() {
        val mock = mockk<InstantReturner>(relaxed = true)

        val formatted =
            assertDoesNotThrow {
                DateTimeFormatter.ISO_INSTANT.format(mock.instant.toJavaInstant())
            }

        assertEquals("1970-01-01T00:00:00Z", formatted)
    }

    /** The general value-class path: relaxed answers already hand back zero values. */
    @Test
    fun `relaxed plain value class property returns zero value`() {
        val mock = mockk<PlainValueReturner>(relaxed = true)

        assertEquals(PlainValue(0), mock.plain)
    }

    @Test
    fun `relaxed plain value class function returns zero value`() {
        val mock = mockk<PlainValueReturner>(relaxed = true)

        assertEquals(PlainValue(0), mock.providePlain())
    }

    /** Value class over Long, like the `kotlin.time.Instant` behind kotlinx-datetime 0.7+. */
    @Test
    fun `relaxed Duration property returns zero`() {
        val mock = mockk<DurationReturner>(relaxed = true)

        assertEquals(kotlin.time.Duration.ZERO, mock.duration)
    }

    private fun assertRealEpochInstant(instant: Instant?) {
        assertNotNull(instant)
        assertEquals(Instant.fromEpochMilliseconds(0), instant)
        assertEquals("1970-01-01T00:00:00Z", instant.toString())
        assertEquals(java.time.Instant.EPOCH, instant.toJavaInstant())
    }
}
