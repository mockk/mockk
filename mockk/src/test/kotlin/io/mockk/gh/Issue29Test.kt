package io.mockk.gh

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertFalse


class Issue29Test {
    class MockCls {
        fun op(klass: Class<*>): Boolean = true
    }

    @Test
    fun matchingAnyClass() {
        val mock = mockk<MockCls>()
        every { mock.op(any()) } returns false
        assertFalse(mock.op(Long::class.java))
        verify { mock.op(any()) }
    }
}

