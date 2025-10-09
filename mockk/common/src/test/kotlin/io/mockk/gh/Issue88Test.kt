package io.mockk.gh

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertFailsWith

@Suppress("UNUSED_PARAMETER")
class Issue88Test {
    open class B {}
    class C : B() {}
    class A { fun go(x: B) {} }

    @Test
    fun test1() {
        val mock = mockk<A>()

        every { mock.go(ofType<C>()) } just Runs

        assertFailsWith(MockKException::class) {
            mock.go(B())
        }
    }

    @Test
    fun test2() {
        val mock = mockk<A>()

        every { mock.go(ofType<C>()) } just Runs

        mock.go(C())
    }


    @Test
    fun test3() {
        val mock = mockk<A>()

        every { mock.go(ofType()) } just Runs

        mock.go(B())
    }

    @Test
    fun test4() {
        val mock = mockk<A>()

        every { mock.go(ofType()) } just Runs

        mock.go(C())
    }
}