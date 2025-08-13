package io.mockk.it

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse

/**
 * Test related to GitHub issue #29
 */
@Suppress("UNUSED_PARAMETER")
class JavaClassParamTest {
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
