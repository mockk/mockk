package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class ParametersWhitDefaultTest {

    private val target = mockk<DefaultParam>()

    /**
     * See issue #312.
     *
     * Mocking a function with default parameter should match without specifying its
     * parameters.
     */
    @Test
    fun testAgainstDefaultParam() {
        every { target.foo() } returns STUB_STRING

        assertEquals(target.foo(), STUB_STRING)
    }

    private companion object {
        const val STUB_STRING = "A string"
    }

    interface DefaultParam {
        fun foo(param: String = "default"): String
    }
}
