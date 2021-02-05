package io.mockk.gh

import io.mockk.MockKException
import io.mockk.every
import kotlin.test.Test
import kotlin.test.assertFailsWith

class Issue538Test {

    @Test
    fun `throw exception if not mocked class is in every block`() {
        val expectedExceptionMessage =
            "Missing mocked calls inside every { ... } block: make sure the object inside the block is a mock"
        assertFailsWith<MockKException>(expectedExceptionMessage) {
            val notMockedClass = NotMockedClass()
            every { notMockedClass.methodThatReturnsANumber() } returns 22
        }
    }

    private class NotMockedClass {
        fun methodThatReturnsANumber() = 55
    }
}
