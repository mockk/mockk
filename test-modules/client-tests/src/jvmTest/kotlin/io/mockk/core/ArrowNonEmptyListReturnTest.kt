package io.mockk.core

import arrow.core.NonEmptyList
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

// https://github.com/mockk/mockk/issues/1410
class ArrowNonEmptyListReturnTest {
    private class Demo {
        fun foo(): String = "foo"

        fun failsToMock(): NonEmptyList<Any> = error("not implemented")
    }

    @Test
    fun `verify function returning NonEmptyList can be mocked`() {
        val mockedDemo = mockk<Demo>()
        every { mockedDemo.foo() } returns "bar"

        assertEquals("bar", mockedDemo.foo())
    }
}
