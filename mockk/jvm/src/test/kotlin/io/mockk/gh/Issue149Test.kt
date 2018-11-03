package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue149Test {
    class MockCls {
        val ret: Int = 5
    }

    @Test
    fun testBackingFields() {
        val obj = mockk<MockCls>()

        every { obj.ret } returns 3

        assertEquals(
            3,
            obj
                .javaClass
                .getDeclaredField("ret")
                .also { it.isAccessible = true }
                .get(obj)
        )
    }
}