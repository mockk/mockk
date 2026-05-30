package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals

// https://github.com/mockk/mockk/issues/1326
sealed interface SealedInput

@JvmInline
value class WrappedString(
    val value: String?,
) : SealedInput

@JvmInline
value class WrappedNonNull(
    val value: String,
) : SealedInput

interface Processor {
    fun process(input: SealedInput): String
}

class SealedInterfaceWithValueClassTest {
    @Test
    fun `capturing argument of sealed interface with nullable-inner value class does not throw`() {
        val mock = mockk<Processor>()
        val slot = slot<SealedInput>()

        every { mock.process(capture(slot)) } returns "ok"

        val result = mock.process(WrappedString(null))
        assertEquals("ok", result)
        assertEquals(WrappedString(null), slot.captured)
    }

    @Test
    fun `capturing argument of sealed interface with non-null-inner value class does not throw`() {
        val mock = mockk<Processor>()
        val slot = slot<SealedInput>()

        every { mock.process(capture(slot)) } returns "ok"

        val result = mock.process(WrappedNonNull("hello"))
        assertEquals("ok", result)
        assertEquals(WrappedNonNull("hello"), slot.captured)
    }

    @Test
    fun `matching sealed interface argument returns correct stub`() {
        val mock = mockk<Processor>()

        every { mock.process(any()) } returns "matched"

        assertEquals("matched", mock.process(WrappedString(null)))
        assertEquals("matched", mock.process(WrappedNonNull("hi")))
    }

    @Test
    fun `relaxed mock with sealed interface and value class subclass returns default`() {
        val mock = mockk<Processor>(relaxed = true)
        val result = mock.process(WrappedString(null))
        assertEquals("", result)
    }
}
