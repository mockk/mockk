package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test related to GitHub issue #94
 */
class LocaleTest {
    @Test
    fun shouldMockLocale() {
        val foo = mockk<Foo> {
            every { getLocale() } returns Locale.GERMAN
        }

        assertEquals(Locale.GERMAN, foo.getLocale())
    }

    interface Foo {
        fun getLocale(): Locale
    }
}
