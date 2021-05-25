package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * see issue #36
 */
class IntTest {
    abstract class Cls1<out R> {
        abstract fun op(): R
    }

    class Cls3<R>(val c: Cls1<R>) {
        var res: R? = null
        fun op2() {
            res = c.op()
        }
    }

    @Test
    fun intReturnTypeThrowsErrorFromGeneric() {
        val mock: Cls1<Int> = mockk()
        every { mock.op() } throws RuntimeException("error")

        val wrapper = Cls3(mock)
        assertFailsWith<RuntimeException> { wrapper.op2() }
        assertEquals(null, wrapper.res)
    }

    @Test
    fun intReturnTypeFromGeneric() {
        val mock: Cls1<Int> = mockk()
        every { mock.op() } returns 22

        val wrapper = Cls3(mock)
        wrapper.op2()
        assertEquals(22, wrapper.res)
    }

    @Test
    fun numberUnboxing() {
        val mock: Number = mockk()

        every { mock.toInt() } returns 3

        assertEquals(3, mock.toInt())
    }
}
