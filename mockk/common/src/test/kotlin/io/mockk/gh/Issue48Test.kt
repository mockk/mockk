package io.mockk.gh

import io.mockk.InternalPlatformDsl
import io.mockk.verify
import io.mockk.every
import io.mockk.spyk
import io.mockk.mockk
import kotlin.test.Test

@Suppress("UNUSED_PARAMETER")
class Issue48Test {
    class MyClass {
        fun publicCall() {
            InternalPlatformDsl.runCoroutine {
                myPrivateCall(5)
            }
        }

        private suspend fun myPrivateCall(arg1: Int): String {
            println("PRVRV")
            return ""
        }
    }

    @Test
    fun test() {
        val myClassSpy = spyk<MyClass>(recordPrivateCalls = true)

        // every { myClassSpy["myPrivateCall"](5) } returns "something"

        myClassSpy.publicCall()

        verify { myClassSpy["myPrivateCall"](5) }
    }
}
