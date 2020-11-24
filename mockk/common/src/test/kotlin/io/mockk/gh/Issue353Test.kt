package io.mockk.gh

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue353Test {

    @Test
    fun testNullableCapture() {
        class Mock {
            fun call(_: String?) {
            }
        }

        val list = mutableListOf<String?>()
        val test: Mock = mockk {
            every { call(captureNullable(list)) } just Runs
        }

        val args = listOf("One", "Two", "Three", null)
        for (arg in args) {
            test.call(arg)
        }

        assertEquals(args, list)
    }
}
