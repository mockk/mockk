package io.mockk.test

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BasicClientTest {
    class Sut(
        private val collaborator: Collaborator
    ) {
        fun aFunction(): String = collaborator.anotherFunction("world")
    }

    class Collaborator {
        fun anotherFunction(param: String): String = "hello $param"
    }

    @Test
    fun aVeryBasicClientTest() {
        val collab = mockk<Collaborator>()
        every { collab.anotherFunction(any()) } returns "hello everyone"

        val sut = Sut(collab)
        assertEquals("hello everyone", sut.aFunction())
    }
}
