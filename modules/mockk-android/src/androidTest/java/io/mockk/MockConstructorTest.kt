package io.mockk

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MockConstructorTest {
    class MockCls {
        fun add(a: Int, b: Int) = a + b
        companion object {
            const val VALUE = 5
        }
    }

    @Test
    fun testMockkConstructorClassWithCompanion() {
        mockkConstructor(MockCls::class)
        every { anyConstructed<MockCls>().add(1, 2) } returns 4
        assertEquals(4, MockCls().add(1, 2))
    }
}