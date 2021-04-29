package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

interface DefaultParam {
    fun foo(param: String = "default"): String
}

class Issue312Test {

    private val target = mockk<DefaultParam>()

    /**
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
}
