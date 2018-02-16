package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PrivateMethodsTest {
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
        val mock = spyk<Abc>()
        every { mock["x"]() } returns "def"
        assertEquals("def", mock.y())
        verify {
            mock["x"]()
            mock.y()
        }
    }

    @Test
    fun objectPrivateMethod() {
        objectMockk(Def).use {
            every { Def["x"]() } returns "ghi"
            assertEquals("ghi", Def.y())
            verify {
                Def["x"]()
                Def.y()
            }
        }
    }


}