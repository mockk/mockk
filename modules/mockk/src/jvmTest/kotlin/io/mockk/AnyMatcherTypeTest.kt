package io.mockk

import kotlin.test.Test
import kotlin.test.assertFails

class AnyMatcherTypeTest {
    private interface Sender {
        fun send(message: Any)
    }

    private fun codeUnderTest(sender: Sender) {
        sender.send("foo")
    }

    @Test
    fun `any reified type should respect classifier when arg is Any`() {
        val sender = mockk<Sender>()
        every { sender.send(any()) } just Runs

        codeUnderTest(sender)

        assertFails {
            verify { sender.send(any<Int>()) }
        }
    }

    @Test
    fun `any KClass should respect classifier when arg is Any`() {
        val sender = mockk<Sender>()
        every { sender.send(any()) } just Runs

        codeUnderTest(sender)

        assertFails {
            verify { sender.send(any(Int::class)) }
        }
    }

    @Test
    fun `capture slot respects type when arg is Any`() {
        val sender = mockk<Sender>()
        every { sender.send(any()) } just Runs

        codeUnderTest(sender)

        val s = slot<Int>()
        assertFails {
            verify { sender.send(capture(s)) }
        }
    }

    @Test
    fun `any reified type should match correct type when arg is Any`() {
        val sender = mockk<Sender>()
        every { sender.send(any()) } just Runs

        sender.send(123)

        verify { sender.send(any<Int>()) }
    }

    @Test
    fun `any Any class should match when arg is Any`() {
        val sender = mockk<Sender>()
        every { sender.send(any()) } just Runs

        codeUnderTest(sender)

        verify { sender.send(any(Any::class)) }
    }
}
