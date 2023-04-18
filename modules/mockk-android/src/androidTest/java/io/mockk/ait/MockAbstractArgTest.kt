package io.mockk.ait

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import org.junit.runner.RunWith
import kotlin.test.Test

interface IOtherInterface {}

interface IMockableInterface {
    fun doSomethingWithAbstractClass(a: MyAbstractClass?)
    fun doSomethingWithInterface(a: IOtherInterface?)
    fun doSomethingWithString(s: String)
}

@RunWith(AndroidJUnit4::class)
class MockAbstractArgTest {

    @Test
    fun canVerifyStringArg() {
        val myMock = mockk<IMockableInterface>(relaxUnitFun = true)

        myMock.doSomethingWithString("hello")

        // works
        verify { myMock.doSomethingWithString(any()) }
    }

    @Test
    fun cannotVerifyAbstractArg() {
        val myMock = mockk<IMockableInterface>(relaxUnitFun = true)

        myMock.doSomethingWithAbstractClass(null)

        // JNI DETECTED ERROR IN APPLICATION: can't make objects of type com.test.MyAbstractClass
        verify { myMock.doSomethingWithAbstractClass(any()) }
    }

    @Test
    fun canVerifyInterfaceArg() {
        val myMock = mockk<IMockableInterface>(relaxUnitFun = true)

        myMock.doSomethingWithInterface(null)

        // works
        verify { myMock.doSomethingWithInterface(any()) }
    }
}
