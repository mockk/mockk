package io.mockk.gh

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue353Test {
    @Test
    fun `captureNullable should be able to capture null`() {
        class MockedClass {
            fun call(arg: String?) {}
        }

        val captures = mutableListOf<String?>()
        val mock = mockk<MockedClass> {
            every { call(captureNullable(captures)) } just Runs
        }

        mock.call(null)

        assertEquals(captures, listOf<String?>(null))
    }
}


