package io.mockk

import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

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

    companion object {
        @JvmInline
        value class Token(
            val raw: String,
        )

        interface ThrowTestService {
            fun process(input: String)
        }

        interface ValueThrowTestService {
            fun useToken(token: Token)
        }

        interface GenericPublisher<T : Any> {
            fun publish(message: T)
        }

        interface Encrypter {
            fun encrypt(message: String): String
        }

        interface PublisherWithDefault<T : Any> {
            fun publish(
                message: T,
                otherParam: String,
            )

            fun publish(message: T) {
                publish(message, "default")
            }
        }
    }

    @Test
    fun `every throws with any() should register exception for non-value-class`() {
        val service = mockk<ThrowTestService>()
        val ex = RuntimeException("test error")
        every { service.process(any()) } throws ex

        assertFailsWith<RuntimeException> {
            service.process("hello")
        }
    }

    @Test
    fun `every throws with any() should register exception for value class`() {
        val service = mockk<ValueThrowTestService>()
        val ex = RuntimeException("test error")
        every { service.useToken(any()) } throws ex

        assertFailsWith<RuntimeException> {
            service.useToken(Token("abc"))
        }
    }

    @Test
    fun `every returns with any() should work for value class`() {
        val service = mockk<ValueThrowTestService>()
        every { service.useToken(any()) } returns Unit

        service.useToken(Token("abc"))

        verify { service.useToken(any<Token>()) }
    }

    @Test
    fun `relaxed mock every throws with any() on generic interface`() {
        val publisher = mockk<GenericPublisher<String>>(relaxed = true)
        val ex = RuntimeException("test error")
        every { publisher.publish(any()) } throws ex

        assertFailsWith<RuntimeException> {
            publisher.publish("hello")
        }
    }

    @Test
    fun `relaxed mock every throws with any() when arg comes from other relaxed mock`() {
        val encrypter = mockk<Encrypter>(relaxed = true)
        val publisher = mockk<GenericPublisher<String>>(relaxed = true)
        val ex = RuntimeException("test error")
        every { publisher.publish(any()) } throws ex

        val encrypted = encrypter.encrypt("some message")

        assertFailsWith<RuntimeException> {
            publisher.publish(encrypted)
        }
    }

    @Test
    fun `relaxed mock every throws with any() on interface with default method`() {
        val encrypter = mockk<Encrypter>(relaxed = true)
        val publisher = mockk<PublisherWithDefault<String>>(relaxed = true)
        val ex = RuntimeException("test error")
        every { publisher.publish(any()) } throws ex

        val encrypted = encrypter.encrypt("some message")

        assertFailsWith<RuntimeException> {
            publisher.publish(encrypted)
        }
    }
}
