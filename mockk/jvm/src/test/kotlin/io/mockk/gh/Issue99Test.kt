package io.mockk.gh

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Issue99Test {
    @Test
    fun someTest() {
        val timestamp = Instant.now().toEpochMilli()
        assertNotEquals(123L, timestamp)
    }

    @Test
    fun someOtherTest() {
        mockkStatic(Instant::class)
        every { Instant.now().toEpochMilli() } returns 123

        val timestamp = Instant.now().toEpochMilli()
        assertEquals(123, timestamp)

        unmockkStatic(Instant::class)
    }
}
