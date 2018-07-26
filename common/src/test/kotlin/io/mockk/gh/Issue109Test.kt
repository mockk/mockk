package io.mockk.gh

import io.mockk.*
import kotlin.test.Test

class Issue109Test {
    class Bar {
        fun baz(foo: String) {
            println(foo)
        }
    }

    class Foo {
        override fun toString(): String {
            return "foo"
        }
    }

    @Test
    fun test() {
        val foo = mockk<Foo>()
        val bar = mockk<Bar>()

        every { bar.baz("$foo") } just runs

        bar.baz("$foo")

        verify(exactly = 1) { bar.baz("$foo") }

    }
}