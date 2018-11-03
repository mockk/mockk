package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test

class Issue158Test {
    class TestClass {
        fun alwaysThrows() : Nothing {
            throw RuntimeException("this can be any exception")
        }
    }

    val targetClass = mockk<TestClass>()

    @Test
    fun testNothingIsNotThrowingNPE() {
        every { targetClass.alwaysThrows() } answers {
            throw IllegalArgumentException("this is a test")
        }
    }
}