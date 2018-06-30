package io.mockk.gh

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Issue98Test {
    @Test
    fun someTest() {
        mockkStatic(System::class)

        every { System.getProperty(any()) } returns "value"

        assertEquals(System.getProperty("abc"), "value")

        unmockkStatic(System::class)
    }
}
