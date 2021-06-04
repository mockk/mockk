package io.mockk.it

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyAll
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class VerifyTest {

    val mock = mockk<MockCls>()

    fun doCalls() {
        every { mock.op(5) } returns 1
        every { mock.op(6) } returns 2
        every { mock.op(7) } returns 3

        assertEquals(1, mock.op(5))
        assertEquals(2, mock.op(6))
        assertEquals(3, mock.op(7))
    }

    @Test
    fun checkVerify() {
        doCalls()

        verify {
            mock.op(6)
            mock.op(5)
        }
    }

    @Test
    fun checkVerifyInverse1() {
        doCalls()

        verify(inverse = true) {
            mock.op(6)
            mock.op(8)
        }
    }

    @Test
    fun checkVerifyInverse2() {
        doCalls()

        verify(inverse = true) {
            mock.op(4)
            mock.op(8)
        }
    }

    @Test
    fun checkVerifyOrder1() {
        doCalls()

        verifyOrder {
            mock.op(5)
            mock.op(7)
        }
    }

    @Test
    fun checkVerifyOrder2() {
        doCalls()

        verifyOrder {
            mock.op(5)
            mock.op(6)
        }
    }

    @Test
    fun checkVerifyOrder3() {
        doCalls()

        verifyOrder {
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun checkVerifyOrderInverse1() {
        doCalls()

        verifyOrder(inverse = true) {
            mock.op(7)
            mock.op(5)
        }
    }

    @Test
    fun checkVerifyOrderInverse2() {
        doCalls()

        verifyOrder(inverse = true) {
            mock.op(5)
            mock.op(4)
        }
    }

    @Test
    fun checkVerifyOrderInverse3() {
        doCalls()

        verifyOrder(inverse = true) {
            mock.op(4)
            mock.op(8)
        }
    }

    @Test
    fun verifySequence() {
        doCalls()

        verifySequence {
            mock.op(5)
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun verifySequenceInverse1() {
        doCalls()

        verifySequence(inverse = true) {
            mock.op(6)
            mock.op(7)
        }
    }

    @Test
    fun verifySequenceInverse2() {
        doCalls()

        verifySequence(inverse = true) {
            mock.op(7)
            mock.op(6)
            mock.op(5)
        }
    }

    @Test
    fun verifySequenceInverse3() {
        doCalls()

        verifySequence(inverse = true) {
            mock.op(6)
            mock.op(5)
            mock.op(7)
        }
    }

    /**
     * See issue #109
     */
    @Test
    fun verifyWithToString() {
        val foo = mockk<Foo>()
        val bar = mockk<Bar>()

        every { bar.baz("$foo") } just runs

        bar.baz("$foo")

        verify(exactly = 1) { bar.baz("$foo") }

    }

    /**
     * See issue #389.
     */
    @Test
    @Ignore
    // Temporarily ignored because it suddenly started failing only on Github actions
    internal fun verifyUsingVerifyAll() {
        val repositoryMock = mockk<TweetRepository>(relaxed = true)

        repositoryMock.persist(Tweet(1, "first tweet"))
        repositoryMock.persist(Tweet(2, "second tweet"))


        verifyAll {
            repositoryMock.persist(
                withArg {
                    assertEquals(it.id, 1)
                    assertEquals(it.text, "first tweet")
                })
            repositoryMock.persist(
                withArg {
                    assertEquals(it.id, 2)
                    assertEquals(it.text, "second tweet")
                })
        }
    }

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

    class MockCls {
        fun op(a: Int) = a + 1
    }

    class Tweet(val id: Int, val text: String)

    interface TweetRepository {

        fun persist(tweet: Tweet)

    }
}
