package io.mockk.gh

import io.mockk.mockk
import io.mockk.verifyOrder
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Issue614Test {

    @Test
    fun `verifyOrder should throw AssertionError if no matching is found`() {
        val mock: Something = mockk(relaxed = true, relaxUnitFun = true)

        assertFailsWith<AssertionError> { verifyOrder { mock.doSomething() } }
    }

    open class Something {
        fun doSomething() {}
    }
}
