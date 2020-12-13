// this issue occurred when package is not in io.mockk.*
package io.github.mockk.gh

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class Issue492Test {
    @Test
    fun `run on RobolectricTestRunner should success`() {
        val generic: Generic<Implemented> = mockk {
            every { sealed } returns Implemented("test")
        }

        // verify
        assertNotNull(generic.sealed)
    }
}

class Generic<T: Sealed>(val sealed: T)
sealed class Sealed
data class Implemented(val ignored: String): Sealed()
