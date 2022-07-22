package io.mockk.it

import io.mockk.*
import kotlin.jvm.JvmInline
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueClassTest {

    private val mock = mockk<DummyService>()

    @Test
    fun valueClassObjectAsReturnValue() {
        every { mock.requestValue() } returns DummyValue(42)

        assertEquals(DummyValue(42), mock.requestValue())

        verify { mock.requestValue() }
    }

    @Test
    fun valueClassObjectAsFunctionArgumentAndReturnValue() {
        every { mock.processValue(DummyValue(1)) } returns DummyValue(42)

        assertEquals(DummyValue(42), mock.processValue(DummyValue(1)))

        verify { mock.processValue(DummyValue(1)) }
    }

    @Test
    fun valueClassObjectAsFunctionArgumentAndAnswerValue() {
        every { mock.processValue(DummyValue(1)) } answers { DummyValue(42) }

        assertEquals(DummyValue(42), mock.processValue(DummyValue(1)))

        verify { mock.processValue(DummyValue(1)) }
    }

    @Test
    fun anyValueClassMatcherAsFunctionArgumentAndValueClassObjectAsReturnValue() {
        every { mock.processValue(any()) } returns DummyValue(42)

        assertEquals(DummyValue(42), mock.processValue(DummyValue(1)))

        verify { mock.processValue(DummyValue(1)) }
    }

    @Test
    fun `any matcher for value class`() {
        val mock = mockk<ValueServiceDummy>(relaxed = true)
        val givenResult = 1
        every { mock.doSomething(any()) } returns givenResult

        val result = mock.doSomething(ValueDummy("moin"))

        assertEquals(givenResult, result)
    }

    @Test
    fun `slot for value class`() {
        val mock = mockk<ValueServiceDummy>(relaxed = true)
        val slot = slot<ValueDummy>()
        val givenResult = 1
        every { mock.doSomething(capture(slot)) } returns givenResult

        val givenParameter = ValueDummy("s")

        val result = mock.doSomething(givenParameter)

        assertEquals(givenResult, result)
        assertEquals(givenParameter, slot.captured)
    }

    @Test
    fun `value class as return value`() {
        val mock = mockk<ValueServiceDummy>(relaxed = true)
        val givenResult = ValueDummy("moin")
        every { mock.getSomething() } returns givenResult

        val result = mock.getSomething()

        assertEquals(givenResult, result)
    }
}

// TODO should be value class in kotlin 1.5+
inline class DummyValue(val value: Int)

class DummyService {

    fun requestValue()  = DummyValue(0)

    fun processValue(value: DummyValue)  = DummyValue(0)
}

@JvmInline
value class ValueDummy(val value: String)

interface ValueServiceDummy {
    fun doSomething(value: ValueDummy): Int
    fun getSomething(): ValueDummy
}
