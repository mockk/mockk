package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Issue36Test {
    class MockCls<R> {
        fun op(): R = throw RuntimeException()
    }

    class Wrapper<R>(val c: MockCls<R>) {
        var res: R? = null
        fun op2() {
            res = c.op()
        }
    }

    @Test
    @Ignore
    fun intReturnTypeFromGeneric() {
        val mock: MockCls<Int> = mockk()
        every { mock.op() } throws RuntimeException("error")

        val wrapper = Wrapper(mock)
        assertFailsWith<RuntimeException> { wrapper.op2() }
        assertEquals(null, wrapper.res)
    }
}
