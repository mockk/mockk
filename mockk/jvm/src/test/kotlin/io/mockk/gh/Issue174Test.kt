@file:Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER", "UNNECESSARY_SAFE_CALL")

package io.mockk.gh

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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
            myPossiblyNullInstance?.doSomething()
        }
    }
}