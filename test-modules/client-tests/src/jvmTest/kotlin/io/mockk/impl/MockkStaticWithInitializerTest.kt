package io.mockk.impl

import io.mockk.core.ClassWithStaticField
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MockkStaticWithInitializerTest {
    @Test
    fun `should be able to mockk static a class with static fields with coverage `() {
        mockkStatic(ClassWithStaticField::class)
        every { ClassWithStaticField.instance().foo() } returns 12

        assertEquals(12, ClassWithStaticField.instance().foo())

        verify(exactly = 1) { ClassWithStaticField.instance().foo() }
        unmockkStatic(ClassWithStaticField::class)
        assertEquals(10, ClassWithStaticField.instance().foo())
    }
}
