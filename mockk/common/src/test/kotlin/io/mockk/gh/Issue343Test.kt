package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue343Test {
    class MockCls {
        fun op() = 0
    }

    val mock = mockk<MockCls>()

    @Test
    fun andThenAnswer() {
        // Setup
        val firstAnswers = 1
        val secondAnswer = 2
        every { mock.op() } answers { firstAnswers } andThenAnswer { secondAnswer }
        // Execute and Assert
        assertEquals(firstAnswers, mock.op())
        assertEquals(secondAnswer, mock.op())
    }
}
