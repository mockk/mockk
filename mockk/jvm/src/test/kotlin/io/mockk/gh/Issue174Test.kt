package io.mockk.gh

import io.mockk.mockk
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test

class Issue174Test {
    class MyClass {
        suspend fun doSomething() {

        }
    }

    @Test
    fun test() {
        runBlocking {
            var myPossiblyNullInstance: MyClass? = null
            myPossiblyNullInstance = mockk(relaxed = true)
            @Suppress("UNNECESSARY_SAFE_CALL")
            myPossiblyNullInstance?.doSomething()
        }
    }
}