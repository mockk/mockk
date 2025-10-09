package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNUSED_PARAMETER")
class PrivateFunctionsTest {
    class Abc {
        fun y() = x()

        private fun x() = "abc"
    }

    object Def {
        fun y() = x()

        private fun x() = "abc"
    }

    class MockCls {
        fun y(a: Int, b: Int?, d: Def?) = x(a, b, d)

        private fun x(a: Int, b: Int?, d: Def?) = "abc $a $b"
    }

    @Test
    fun spyPrivateMethod() {
        val mock = spyk<Abc>(recordPrivateCalls = true)
        every { mock["x"]() } returns "def"
        assertEquals("def", mock.y())
        verifySequence {
            mock.y()
            mock["x"]()
        }
    }

    @Test
    fun objectPrivateMethod() {
        mockkObject(Def, recordPrivateCalls = true) {
            every { Def["x"]() } returns "ghi"
            assertEquals("ghi", Def.y())
            verify {
                Def.y()
                Def["x"]()
            }
        }
    }

    @Test
    fun spyNoRecordingPrivateMethod() {
        val mock = spyk<Abc>()
        every { mock["x"]() } returns "def"
        assertEquals("def", mock.y())
        verifySequence {
            mock.y()
        }
    }

    @Test
    fun objectNoRecordingPrivateMethod() {
        mockkObject(Def) {
            every { Def["x"]() } returns "ghi"
            assertEquals("ghi", Def.y())
            verifySequence {
                Def.y()
            }
        }
    }

    @Test
    fun privateCallsWithNullability() {
        val mock = spyk<MockCls>(recordPrivateCalls = true)
        every { mock["x"](any<Int>(), any<Int>(), any<Def>()) } returns "test"

        assertEquals("test", mock.y(1, 2, null))

        verify { mock["x"](any<Int>(), any<Int>(), any<Def>()) }
    }

}