package io.mockk.it

import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Regression tests for unnecessary stubbing detection when a getter stub works through
 * the backing field, see https://github.com/mockk/mockk/issues/1345
 */
class UnnecessaryStubbingBackingFieldTest {
    data class Multiplier(
        val value: Int,
    )

    class Calculator(
        val multiplier: Multiplier,
    ) {
        fun multiply(a: Int): Int = a * multiplier.value
    }

    /** https://github.com/mockk/mockk/issues/1345 */
    @Test
    fun `getter stub consumed through the backing field is not reported as unnecessary`() {
        val calculator = mockk<Calculator>()
        every { calculator.multiply(2) } answers { callOriginal() }

        val multiplier = mockk<Multiplier>()
        every { calculator.multiplier } returns multiplier
        every { multiplier.value } returns 10

        assertEquals(20, calculator.multiply(2))

        checkUnnecessaryStub(calculator)
        checkUnnecessaryStub(multiplier)
    }

    @Test
    fun `non-constant getter stub that is never intercepted is still reported as unnecessary`() {
        val calculator = mockk<Calculator>()
        val multiplier = mockk<Multiplier>()

        // An answers { ... } stub is not written into the backing field, so it must not
        // be marked as used by the backing field assignment.
        every { calculator.multiplier } answers { multiplier }

        assertFailsWith<AssertionError> {
            checkUnnecessaryStub(calculator)
        }
    }

    @Test
    fun `getter stub used through a direct call is not reported as unnecessary`() {
        val calculator = mockk<Calculator>()
        val multiplier = mockk<Multiplier>()
        every { calculator.multiplier } returns multiplier
        every { multiplier.value } returns 10

        assertEquals(10, calculator.multiplier.value)

        checkUnnecessaryStub(calculator)
        checkUnnecessaryStub(multiplier)
    }
}
