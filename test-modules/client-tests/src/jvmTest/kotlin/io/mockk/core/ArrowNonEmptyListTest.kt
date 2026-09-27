package io.mockk.core

import arrow.core.NonEmptyList
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ArrowNonEmptyListTest {

    @Test
    fun `verify function returning NonEmptyList can be mocked`() {
        val mockedDemo = mockk<Demo>()
        every { mockedDemo.foo() } returns "bar"

        assertEquals("bar", mockedDemo.foo())
    }

}

private class Demo {
    fun foo(): String = "foo"
    fun failsToMock(): NonEmptyList<Any> = error("not implemented")
}