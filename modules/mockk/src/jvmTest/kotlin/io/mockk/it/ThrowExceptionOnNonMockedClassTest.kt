package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

/**
 * Test related to GitHub issue #538
 */
class ThrowExceptionOnNonMockedClassTest {

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
