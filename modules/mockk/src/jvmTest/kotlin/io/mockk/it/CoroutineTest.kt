package io.mockk.it

import io.mockk.*
import kotlinx.coroutines.*
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoroutineTest {

    data class ClearMocksClass(val a: String) {
        suspend fun a() = coroutineScope {
            // logic
        }
    }
    /**
     * GitHub issue #234
     */
    @Test
    fun clearMocksTest() {
        mockkConstructor(ClearMocksClass::class)
        coEvery { anyConstructed<ClearMocksClass>().a() } just runs
    }


    /**
     * GitHub issue #288
     */
    @Test
    fun suspendFnMocking() {
        val call = mockk<suspend () -> Int>()
        coEvery { call() } returns 5
        runBlocking { assertEquals(5, call()) }
    }

    /**
     * Begin - GitHub issue #171
     */
    interface Executable {
        suspend fun execute(): String
    }

    @Test
    fun testOk() {
        val executionCompletionSource = CompletableDeferred<String>()
        suspend fun task() = executionCompletionSource.await()
        val mock = mockk<Executable> {
            coEvery { execute() } coAnswers { task() }
        }

        val timeout = AtomicBoolean()
        val done = AtomicBoolean()

        runBlocking {
            val execution = launch {
                done.set(mock.execute() == "first")
            }

            val thread = thread {
                try {
                    Thread.sleep(700)
                } catch (ex: InterruptedException) {
                    return@thread
                }
                timeout.set(true)
                executionCompletionSource.complete("error")
            }
            delay(100)
            executionCompletionSource.complete("first")
            execution.join()
            thread.interrupt()
            assertFalse(timeout.get(), "Failed to cancel execution")
            assertTrue(done.get(), "Failed to finish execution")
        }
    }

    @Test
    fun testCancellation() {
        val executionCompletionSource = CompletableDeferred<String>()
        suspend fun task() = executionCompletionSource.await()
        val mock = mockk<Executable> {
            coEvery { execute() } coAnswers { task() }
        }

        val timeout = AtomicBoolean()

        runBlocking {
            val execution = launch {
                mock.execute()
            } // This blocks the test indefinitely
            val thread = thread {
                try {
                    Thread.sleep(700)
                } catch (ex: InterruptedException) {
                    return@thread
                }
                timeout.set(true)
                executionCompletionSource.cancel()
            }
            delay(100)
            executionCompletionSource.cancel()
            execution.join()
            thread.interrupt()
            assertFalse(timeout.get(), "Failed to cancel execution")
        }
    }

    @Test
    fun testOkAndThenThrow() {
        val executionCompletionSource = CompletableDeferred<String>()
        suspend fun task() = executionCompletionSource.await()
        val mock = mockk<Executable> {
            coEvery { execute() } coAnswers { task() } andThenThrows RuntimeException("test")
        }

        val timeout = AtomicBoolean()
        val done = AtomicBoolean()

        runBlocking {
            val execution = launch {
                if (mock.execute() != "first") {
                    return@launch
                }

                try {
                    mock.execute()
                } catch (ex: RuntimeException) {
                    done.set(true)
                }
            }

            val thread = thread {
                try {
                    Thread.sleep(700)
                } catch (ex: InterruptedException) {
                    return@thread
                }
                timeout.set(true)
                executionCompletionSource.complete("timeout")
            }
            delay(100)
            executionCompletionSource.complete("first")
            execution.join()
            thread.interrupt()
            assertFalse(timeout.get(), "Failed to cancel execution")
            assertTrue(done.get(), "Failed to finish execution/execute andThen")
        }
    }

    @Test
    fun testOkCoAndThen() {
        val executionCompletionSource1 = CompletableDeferred<String>()
        val executionCompletionSource2 = CompletableDeferred<String>()
        suspend fun task1() = executionCompletionSource1.await()
        suspend fun task2() = executionCompletionSource2.await()
        val mock = mockk<Executable> {
            coEvery { execute() } coAnswers { task1() } coAndThen { task2() }
        }

        val timeout = AtomicBoolean()
        val done = AtomicBoolean()

        runBlocking {
            val execution = launch {
                done.set(
                    mock.execute() == "first" &&
                            mock.execute() == "second"
                )
            }

            val thread = thread {
                try {
                    Thread.sleep(700)
                } catch (ex: InterruptedException) {
                    return@thread
                }
                executionCompletionSource1.complete("first")
                executionCompletionSource2.complete("second")
                timeout.set(true)
            }
            delay(100)
            executionCompletionSource1.complete("first")
            executionCompletionSource2.complete("second")
            execution.join()
            thread.interrupt()
            assertFalse(timeout.get(), "Failed to cancel execution")
            assertTrue(done.get(), "Failed to finish execution/execute coAndThen")
        }
    }
    /**
     * End - GitHub issue #171
     */

}
