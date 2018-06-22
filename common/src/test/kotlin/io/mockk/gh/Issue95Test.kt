package io.mockk.gh

import io.mockk.every
import io.mockk.spyk
import kotlin.test.Test

class Issue95Test {
    class Foo {

        fun getInterface(list: List<Boolean>) = listOf(true)

        fun bar(t: String, list: List<Boolean> = getInterface(listOf(false))): String {
            return "FOO"
        }

        fun bar2(list: List<Boolean> = getInterface(listOf(false))): String {
            return "FOO"
        }
    }

    @Test
    fun testMock() {
        val mocking = spyk(Foo())

        every {
            mocking.bar(any())
        } returns "FOO MOCKED"

        println(mocking.bar("hello"))
    }

    @Test
    fun test2() {
        val mocking = spyk(Foo())

        every { mocking.bar2() } returns "FOO MOCKED"

        println(mocking.bar2())
    }
}