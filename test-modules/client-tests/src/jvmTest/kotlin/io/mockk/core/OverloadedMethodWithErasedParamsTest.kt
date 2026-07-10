package io.mockk.core

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

// https://github.com/mockk/mockk/issues/1300
class OverloadedMethodWithErasedParamsTest {

    class MyService {
        fun hello(names: List<String>): String {
            throw RuntimeException("real hello(List<String>) must not be called (names=$names)")
        }

        fun hello(ages: List<Int>): List<String> {
            throw RuntimeException("real hello(List<Int>) must not be called (ages=$ages)")
        }
    }

    @Test
    fun `every stubs the correct overload without calling the real method`() {
        val service = mockk<MyService>()

        every { service.hello(any<List<String>>()) } returns "Hello"
        assertEquals("Hello", service.hello(listOf("bob")))

        every { service.hello(any<List<Int>>()) } returns listOf("World")
        assertEquals(listOf("World"), service.hello(listOf(1)))
    }
}
