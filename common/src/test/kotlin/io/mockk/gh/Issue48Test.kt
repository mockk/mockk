package io.mockk.gh

import io.mockk.InternalPlatformDsl
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test

class Issue48Test {
    class MyClass {
        fun publicCall() {
            InternalPlatformDsl.runCoroutine {
                myPrivateCall(5)
            }
        }

        private suspend fun myPrivateCall(arg1: Int) {
        }
    }

    @Test
    fun test() {
        val myClassSpy = spyk(MyClass())

        every { myClassSpy["myPrivateCall"](5) } returns "something"

        myClassSpy.publicCall()

        verify { myClassSpy["myPrivateCall"](5) }
    }
}