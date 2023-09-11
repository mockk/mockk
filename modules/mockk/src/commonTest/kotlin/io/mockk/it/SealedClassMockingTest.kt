package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail

class SealedClassMockingTest {

    @Test
    fun testSealedClassCanBeMocked() {
        val base = mockk<Base> {
            every { i } returns 0
        }
        assertEquals(0, base.i)
    }

    @Test
    fun testActualSealedSubClassCannotBeMockedWithMoreInterfacesBecauseItsFinal() {
        val exception = assertThrows<MockKException> {
            mockk<Base>(moreInterfaces = arrayOf(Runnable::class))
        }
        exception
            .cause
            ?.message
            ?.also { assertContains(it, "More interfaces requested and class is final") }
            ?: fail("Missing cause")
    }

    private sealed class Base {
        abstract val i: Int
    }

    private data class Derived(override val i: Int) : Base()
}
