package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivateFunctionsTest {
    class Abc {
        fun y() = x()

        private fun x() = "abc"
    }

    object Def {
        fun y() = x()

        private fun x() = "abc"
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
        objectMockk(Def, recordPrivateCalls = true).use {
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
        objectMockk(Def).use {
            every { Def["x"]() } returns "ghi"
            assertEquals("ghi", Def.y())
            verifySequence {
                Def.y()
            }
        }
    }


}