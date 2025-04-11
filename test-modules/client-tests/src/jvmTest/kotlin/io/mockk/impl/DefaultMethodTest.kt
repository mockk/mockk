package io.mockk.impl

import io.mockk.core.ClassImplementingPackagePrivateInterfaceWithDefaultMethod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class DefaultMethodTest {
    @Test
    fun `should mock ClassImplementingPackagePrivateInterfaceWithDefaultMethod class`() {
        val obj = mockk<ClassImplementingPackagePrivateInterfaceWithDefaultMethod>()
        every { obj.foo() } returns 12
        assertEquals(12, obj.foo())
    }
}
