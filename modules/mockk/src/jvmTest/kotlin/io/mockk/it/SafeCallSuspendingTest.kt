@file:Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER", "UNNECESSARY_SAFE_CALL", "RedundantSuspendModifier")

package io.mockk.it

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

/**
 * Safe call operator throws a class cast exception on relaxed mock of suspending function.
 * Verifies issue #174.
 */
class SafeCallSuspendingTest {
    class MyClass {
        suspend fun doSomething() {

        }
    }

    @Test
    fun testSafeCall() {
        runBlocking {
            var myPossiblyNullInstance: MyClass? = null
            myPossiblyNullInstance = mockk(relaxed = true)
            myPossiblyNullInstance?.doSomething()
        }
    }
}
