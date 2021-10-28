package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Creating stubs for backing fields visible by reflection.
 * Verifies issue #149.
 */
class BackingFieldsTest {
    class MockCls {
        val ret: Int = 5
    }

    @Test
    fun testBackingFieldsMockK() {
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

    @Test
    fun testBackingFieldsForSpyk() {
        val obj = spyk<MockCls>()

        every { obj.ret } returns 3

        assertEquals(
            5,
            obj
                .javaClass
                .getDeclaredField("ret")
                .also { it.isAccessible = true }
                .get(obj)
        )
    }
}
