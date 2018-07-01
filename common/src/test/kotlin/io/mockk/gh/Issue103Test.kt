package io.mockk.gh

import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Ignore
import kotlin.test.Test

class Issue103Test {
    class MyClass {

        fun myPublicMethod() {
            myPrivateMethod()
        }

        private fun myPrivateMethod() {
        }
    }

    @Test
    fun getAdapter() {
        val myClass = spyk(MyClass(), recordPrivateCalls = true)
        every { myClass invokeNoArgs "myPrivateMethod" } returns Unit

        myClass.myPublicMethod()

        verify {
            myClass invokeNoArgs "myPrivateMethod"
        }
    }
}