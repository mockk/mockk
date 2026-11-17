package io.mockk.it

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Value-class results delivered through answer-scope `callOriginal()`.
 *
 * The compiler erases a non-nullable value-class return to its underlying type and mangles
 * the method name (`charge-xQ1N4Yw(int): int`), so reflectively invoking the original
 * implementation yields the raw underlying value while `callOriginal()` is declared to
 * return the value class. The erased result has to be re-boxed to the declared type before
 * it reaches the answer.
 *
 * https://github.com/mockk/mockk/issues/1643
 */
class ValueClassCallOriginalTest {
    @Test
    fun callOriginalRoundTripsPrimitiveBackedValueClassReturn() {
        val spy = spyk<Payment>()

        every { spy.charge(any()) } answers { callOriginal() }

        assertEquals(Amount(42), spy.charge(Amount(42)))
    }

    @Test
    fun callOriginalRoundTripsReferenceBackedValueClassReturn() {
        val spy = spyk<Payment>()

        every { spy.recipient(any()) } answers { callOriginal() }

        assertEquals(Name("urgent"), spy.recipient(Name("urgent")))
    }

    @Test
    fun callOriginalRoundTripsNestedValueClassReturn() {
        val spy = spyk<Payment>()

        every { spy.book(any()) } answers { callOriginal() }

        assertEquals(Quantity(Amount(5)), spy.book(Quantity(Amount(5))))
    }

    @Test
    fun callOriginalWithPlainReturnReadsValueClassParameter() {
        val spy = spyk<Payment>()

        every { spy.describe(any()) } answers { callOriginal() }

        assertEquals("amount=42", spy.describe(Amount(42)))
    }

    @Test
    fun callOriginalInCoAnswersRoundTripsSuspendValueClassReturn() {
        val spy = spyk<Payment>()

        coEvery { spy.chargeSuspend(any()) } coAnswers { callOriginal() }

        runBlocking {
            assertEquals(Amount(42), spy.chargeSuspend(Amount(42)))
        }
    }

    @Test
    fun spyDefaultDispatchRoundTripsValueClassReturn() {
        val spy = spyk<Payment>()

        assertEquals(Amount(42), spy.charge(Amount(42)))
        assertEquals(Name("urgent"), spy.recipient(Name("urgent")))
    }

    @Test
    fun stubbedConstantAnswerIsUnboxedForCaller() {
        val spy = spyk<Payment>()

        every { spy.charge(any()) } returns Amount(2)

        assertEquals(Amount(2), spy.charge(Amount(42)))
    }

    @Test
    fun objectMockCallOriginalRoundTripsValueClassReturn() {
        mockkObject(InventoryHolder)
        try {
            every { InventoryHolder.pick(any()) } answers { callOriginal() }

            assertEquals(Quantity(Amount(7)), InventoryHolder.pick(Quantity(Amount(7))))
        } finally {
            unmockkObject(InventoryHolder)
        }
    }
}

@JvmInline
value class Amount(
    val value: Int,
)

@JvmInline
value class Name(
    val value: String,
)

@JvmInline
value class Quantity(
    val amount: Amount,
)

class Payment {
    fun charge(amount: Amount): Amount = amount

    fun recipient(name: Name): Name = name

    fun book(quantity: Quantity): Quantity = quantity

    fun describe(amount: Amount): String = "amount=${amount.value}"

    suspend fun chargeSuspend(amount: Amount): Amount = amount
}

object InventoryHolder {
    fun pick(quantity: Quantity): Quantity = quantity
}
