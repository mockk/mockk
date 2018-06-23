package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

expect class Locale
expect fun getGermanyLocale(): Locale

class Issue94Test {
    @Test
    fun shouldMockLocale() {
        val foo = mockk<Foo> {
            every { getLocale() } returns getGermanyLocale()
        }

        assertEquals(getGermanyLocale(), foo.getLocale())
    }
}

interface Foo {
    fun getLocale(): Locale
}

