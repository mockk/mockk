package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ValueClassTest {

    private val mock = mockk<DummyService>()

    @Test
    fun `value class object as return value`() {
        every { mock.requestValue() } returns DummyValue(42)

        assertEquals(DummyValue(42), mock.requestValue())

        verify { mock.requestValue() }
    }

    @Test
    fun `value class object as function argument and return value`() {
        every { mock.processValue(DummyValue(1)) } returns DummyValue(42)

        assertEquals(DummyValue(42), mock.processValue(DummyValue(1)))

        verify { mock.processValue(DummyValue(1)) }
    }

    @Test
    fun `value class object as function argument and answer value`() {
        every { mock.processValue(DummyValue(1)) } answers { DummyValue(42) }

        assertEquals(DummyValue(42), mock.processValue(DummyValue(1)))

        verify { mock.processValue(DummyValue(1)) }
    }

    @Test
    fun `any value class matcher as function argument and value class object as return value`() {
        every { mock.processValue(any()) } returns DummyValue(42)

        assertEquals(DummyValue(42), mock.processValue(DummyValue(1)))

        verify { mock.processValue(DummyValue(1)) }
    }
}

// TODO should be value class in kotlin 1.5+
private inline class DummyValue(val value: Int)

private class DummyService {

    fun requestValue()  = DummyValue(0)

    fun processValue(value: DummyValue)  = DummyValue(0)
}