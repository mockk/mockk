package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class SealedClassMockingTest {

    @Test
    fun testSealedClassCanBeMocked() {
        val base = mockk<Base> {
            every { i } returns 0
        }
        assertEquals(0, base.i)
    }

    private sealed class Base {
        abstract val i: Int
    }

    private data class Derived(override val i: Int) : Base()
}
