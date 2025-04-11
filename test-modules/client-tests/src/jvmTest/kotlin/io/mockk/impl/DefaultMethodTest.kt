package io.mockk.impl

import io.mockk.core.ClassImplementingPackagePrivateInterfaceWithDefaultMethod
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.apache.commons.lang3.RandomStringUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class DefaultMethodTest {
    @Test
    fun `should mock ClassImplementingPackagePrivateInterfaceWithDefaultMethod class`() {
        val obj = mockk<ClassImplementingPackagePrivateInterfaceWithDefaultMethod>()
        every { obj.foo() } returns 12
        assertEquals(12, obj.foo())
    }

    @Test
    fun `should be able to mockk static with coverage `() {
        mockkStatic(RandomStringUtils::class)
        every { RandomStringUtils.secure().nextAlphanumeric(any()) } returns "x"

        assertEquals(RandomStringUtils.secure().nextAlphanumeric(8), "x")

        verify(exactly = 1) { RandomStringUtils.secure().nextAlphanumeric(8) }
        unmockkStatic(RandomStringUtils::class)
    }
}
