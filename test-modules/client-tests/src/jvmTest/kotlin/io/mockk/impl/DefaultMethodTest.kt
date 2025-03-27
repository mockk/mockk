package io.mockk.impl

import io.mockk.core.ClassImplementingInterfaceWithDefaultMethod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class DefaultMethodTest {
    @Test
    fun `should mock ClassImplementingInterfaceWithDefaultMethod class`() {
        val tableImpl = mockk<ClassImplementingInterfaceWithDefaultMethod>()
        every { tableImpl.foo() } returns 12
        assertEquals(12, tableImpl.toString())
    }
}
