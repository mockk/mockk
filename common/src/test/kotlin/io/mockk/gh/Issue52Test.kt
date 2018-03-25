package io.mockk.gh

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.Test

class Issue52Test {
    interface Foo {
        fun foo(vararg args: String, otherArg: String)
        fun foo(args: List<String>, otherArg: String)
    }

    @Test
    fun varargTest() {
        val mock = mockk<Foo>()
        // this works as expected, matching the List<String> version
        every { mock.foo(args = any<List<String>>(), otherArg = any()) } just Runs

        // this does not work, compiles but fails at runtime with:
        // io.mockk.MockKException: Failed matching mocking signature for
        // Foo(#1).foo([529c407ddf5619f7], -4522a650db14c274)
        // left matchers: [any()]
        every { mock.foo(args = *arrayOf(any()), otherArg = any()) } just Runs
    }
}