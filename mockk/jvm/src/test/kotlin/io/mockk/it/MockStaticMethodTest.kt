package io.mockk.it

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Test
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Tests related to github issues #92 | #99
 */
class MockStaticMethodTest {
    /**
     * github issue #92
     */
    @Test
    fun test() {
        val epochSeconds = 123L
        mockkStatic(Instant::class)
        every { Instant.now().epochSecond } returns epochSeconds

        assertEquals(123L, Instant.now().epochSecond)
    }

    /**
     * github issue #99
     */
    @Test
    fun unmockStatic_unmocksStaticMocks() {
        mockkStatic(Instant::class)
        every { Instant.now().toEpochMilli() } returns 123L

        assertEquals(123L, Instant.now().toEpochMilli())

        unmockkStatic(Instant::class)

        assertNotEquals(123L, Instant.now().toEpochMilli())
    }
}