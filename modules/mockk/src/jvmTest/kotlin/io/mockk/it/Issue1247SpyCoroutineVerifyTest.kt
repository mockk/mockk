package io.mockk.it

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Regression tests for coVerify recording the coroutine scaffolding of the co-block
 * instead of (or in addition to) the calls the block declares, see
 * https://github.com/mockk/mockk/issues/1247
 */
class Issue1247SpyCoroutineVerifyTest {
    class TestClass {
        private fun testMethod2() {
        }

        suspend fun testMethod() {
            delay(100)
            testMethod2()
        }
    }

    /**
     * GitHub issue #1247: the spy call made inside the launched coroutine must be
     * verified exactly once.
     */
    @Test
    fun `coVerify on a spy call made in a launched coroutine sees exactly one call`() =
        runTest {
            val testObj = spyk(TestClass())

            launch {
                testObj.testMethod()
            }.join()

            coVerify(exactly = 1) { testObj.testMethod() }
        }

    /**
     * GitHub issue #1247: with a static mock on `Dispatchers`, the dispatcher lookups the
     * coroutine machinery performs to run the coVerify block itself must not become part
     * of the block, otherwise the verification targets `Dispatchers.getDefault()` instead
     * of the declared call.
     */
    class WithStaticDispatchersMock {
        class TestTest {
            suspend fun foo() {
            }
        }

        private val testScheduler = TestCoroutineScheduler()
        private val testDispatcher get() = StandardTestDispatcher(testScheduler)

        private val testClass = spyk(TestTest())

        @OptIn(ExperimentalCoroutinesApi::class)
        @BeforeTest
        fun setUp() {
            mockkStatic(Dispatchers::class)
            Dispatchers.setMain(testDispatcher)
            every { Dispatchers.Default } returns testDispatcher
        }

        @Test
        fun test() =
            runTest {
                testClass.foo()
                coVerify(exactly = 1) { testClass.foo() }
            }

        @AfterTest
        fun clear() {
            unmockkStatic(Dispatchers::class)
        }
    }
}
