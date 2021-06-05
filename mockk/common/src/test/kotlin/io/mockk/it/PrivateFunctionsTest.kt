package io.mockk.it

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("UNUSED_PARAMETER")
class PrivateFunctionsTest {

    /**
     * See issue #70
     */
    @Test
    fun mockPrivateMethodWithGeneric() {
        val mock = spyk<GenericsCls>()

        every {
            mock["updateItemInDb"](any<Long>(), any<String>(), any()) as Unit
        } just Runs

        mock.pubCall()
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

    /**
     * See issue #103
     */
    @Test
    fun mockPrivateMethodThatReturnsNothing() {
        val myClass = spyk(PrivateNoReturnCls(), recordPrivateCalls = true)
        every { myClass invokeNoArgs "myPrivateMethod" } returns Unit

        myClass.myPublicMethod()

        verify {
            myClass invokeNoArgs "myPrivateMethod"
        }
    }

    /**
     * See issue #346
     */
    @Test
    fun justRunsWithPrivateMethod() {
        val mock = spyk<PrivateNoReturnCls>(recordPrivateCalls = true)

        justRun { mock invokeNoArgs "myPrivateMethod" }

        mock.myPublicMethod()
    }

    class PrivateNoReturnCls {

        fun myPublicMethod() {
            myPrivateMethod()
        }

        private fun myPrivateMethod() {
        }
    }

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

    class GenericsCls {
        private fun <T> updateItemInDb(id: Long, column: String, data: T) {
        }

        fun pubCall() {
            updateItemInDb(1L, "abc", "data")
        }
    }
}
