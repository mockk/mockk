package io.mockk.it

import io.mockk.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionFunctionsTest {

    @Test
    fun staticExtensionFunction() {
        mockkStatic("io.mockk.it.ExtensionFunctionsTestKt")

        every {
            IntWrapper(5).f()
        } returns 11

        assertEquals(11, IntWrapper(5).f())
        assertEquals(25, IntWrapper(20).f())

        verify {
            IntWrapper(5).f()
            IntWrapper(20).f()
        }

    }

    @Test
    fun objectExtensionFunction() {
        mockkObject(ExtObj)

        with(ExtObj) {
            every {
                IntWrapper(5).h()
            } returns 11

            assertEquals(11, IntWrapper(5).h())

            verify {
                IntWrapper(5).h()
            }
        }

    }

    @Test
    fun classExtensionFunction() {
        with(mockk<ExtCls>()) {
            every {
                IntWrapper(5).g()
            } returns 11

            assertEquals(11, IntWrapper(5).g())

            verify {
                IntWrapper(5).g()
            }
        }
    }
}

data class IntWrapper(val data: Int)

class ExtCls {
    fun IntWrapper.g() = data + 5
}

object ExtObj {
    fun IntWrapper.h() = data + 5
}

fun IntWrapper.f() = data + 5
