package io.mockk.gh

import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals

class Issue92Test {
    @Test
    fun test() {
        val epochSeconds = 123L
        mockkStatic(Instant::class)
        every { Instant.now().epochSecond } returns epochSeconds

        assertEquals(123L, Instant.now().epochSecond)
    }
}