package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mocking classes that declare two methods with the same name and the same erased parameter
 * types, which the JVM tells apart only by their return type.
 *
 * Kotlin compiles such pairs from overloads whose generic parameters erase to the same type, and
 * from a member function next to an `inline reified` function of the same name. Both methods have
 * to be intercepted, or calls to one of them run the real implementation on the mock.
 */
class SameParameterTypesMethodTest {
    class Greeter {
        fun hello(names: List<String>): String = throw IllegalStateException("real hello(List<String>) called")

        fun hello(ages: List<Int>): List<String> = throw IllegalStateException("real hello(List<Int>) called")
    }

    class Session(
        val params: MutableMap<String, Any?> = mutableMapOf(),
    ) {
        operator fun get(key: String): String? = params[key]?.toString()

        inline operator fun <reified T> get(key: String): T? = params[key] as? T
    }

    @Test
    fun overloadReturningStringIsStubbed() {
        val mock = mockk<Greeter>()

        every { mock.hello(any<List<String>>()) } returns "stubbed"

        assertEquals("stubbed", mock.hello(listOf("bob")))
        verify { mock.hello(listOf("bob")) }
    }

    @Test
    fun overloadReturningListIsStubbed() {
        val mock = mockk<Greeter>()

        every { mock.hello(any<List<Int>>()) } returns listOf("stubbed")

        assertEquals(listOf("stubbed"), mock.hello(listOf(42)))
        verify { mock.hello(listOf(42)) }
    }

    @Test
    fun memberNextToReifiedInlineFunctionIsStubbed() {
        val mock = mockk<Session>(relaxed = true)

        every { mock["key"] } returns "stubbed"

        assertEquals("stubbed", mock["key"])
        verify { mock["key"] }
    }
}
