package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Value-class arguments read back inside answer blocks.
 *
 * The compiler erases value-class parameters to their underlying type and mangles
 * the method name (`charge-xQ1N4Yw(int)`), so the mock receives the unboxed value
 * while the caller passed a boxed value-class instance. The typed answer-scope
 * accessors (`firstArg<T>()` and friends) have to re-box such arguments to the
 * requested type instead of casting the raw erased value.
 *
 * https://github.com/mockk/mockk/issues/1641
 */
class ValueClassArgumentIntrospectionTest {
    @Test
    fun primitiveBackedValueClassFirstArgIsBoxed() {
        val mock = mockk<Payment>()

        every { mock.charge(any()) } answers { firstArg<Amount>() }

        assertEquals(Amount(42), mock.charge(Amount(42)))
    }

    @Test
    fun primitiveBackedValueClassSecondArgIsBoxed() {
        val mock = mockk<Payment>()

        every { mock.transfer(any(), any()) } answers { secondArg<Amount>() }

        assertEquals(Amount(7), mock.transfer(Amount(1), Amount(7)))
    }

    @Test
    fun primitiveBackedValueClassArgByIndexIsBoxed() {
        val mock = mockk<Payment>()

        every { mock.transfer(any(), any()) } answers { arg<Amount>(0) }

        assertEquals(Amount(1), mock.transfer(Amount(1), Amount(7)))
    }

    @Test
    fun primitiveBackedValueClassLastArgIsBoxed() {
        val mock = mockk<Payment>()

        every { mock.transfer(any(), any()) } answers { lastArg<Amount>() }

        assertEquals(Amount(7), mock.transfer(Amount(1), Amount(7)))
    }

    @Test
    fun thirdArgIsBoxedForValueClassParameter() {
        val mock = mockk<Payment>()

        every { mock.split(any(), any(), any()) } answers { thirdArg<Amount>() }

        assertEquals(Amount(3), mock.split(Amount(1), Amount(2), Amount(3)))
    }

    @Test
    fun referenceBackedValueClassFirstArgIsBoxed() {
        val mock = mockk<Messaging>()

        every { mock.send(any()) } answers { firstArg<Tag>() }

        assertEquals(Tag("urgent"), mock.send(Tag("urgent")))
    }

    @Test
    fun nestedValueClassFirstArgIsBoxed() {
        val mock = mockk<Inventory>()

        every { mock.book(any()) } answers { firstArg<Quantity>() }

        assertEquals(Quantity(Amount(5)), mock.book(Quantity(Amount(5))))
    }

    @Test
    fun alreadyBoxedValueClassArgumentIsNotBoxedTwice() {
        val mock = mockk<Audit>()

        every { mock.record(any()) } answers { firstArg<Amount>() }

        assertEquals(Amount(9), mock.record(Amount(9)))
    }

    @Test
    fun plainClassArgumentIsPassedThroughUnchanged() {
        val mock = mockk<Messaging>()

        every { mock.label(any()) } answers { firstArg<String>() }

        assertEquals("plain", mock.label("plain"))
    }

    @Test
    fun nullableValueClassArgumentNullIsPassedThrough() {
        val mock = mockk<Audit>()

        every { mock.recordNullable(any()) } answers { firstArg<Amount?>() }

        assertEquals(null, mock.recordNullable(null))
    }

    @Test
    fun nullableValueClassArgumentValueIsBoxed() {
        val mock = mockk<Audit>()

        every { mock.recordNullable(any()) } answers { firstArg<Amount?>() }

        assertEquals(Amount(4), mock.recordNullable(Amount(4)))
    }

    interface Payment {
        fun charge(amount: Amount): Amount

        fun transfer(
            from: Amount,
            to: Amount,
        ): Amount

        fun split(
            first: Amount,
            second: Amount,
            third: Amount,
        ): Amount
    }

    interface Messaging {
        fun send(tag: Tag): Tag

        fun label(text: String): String
    }

    interface Inventory {
        fun book(quantity: Quantity): Quantity
    }

    interface Audit {
        fun record(amount: Any): Any

        fun recordNullable(amount: Amount?): Amount?
    }

    @JvmInline
    value class Amount(
        val value: Int,
    )

    @JvmInline
    value class Tag(
        val label: String,
    )

    @JvmInline
    value class Quantity(
        val amount: Amount,
    )
}
