package io.mockk.it

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoroutineTest {
    data class ClearMocksClass(
        val a: String,
    ) {
        suspend fun a() =
            coroutineScope {
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
        val mock =
            mockk<Executable> {
                coEvery { execute() } coAnswers { task() }
            }

        val timeout = AtomicBoolean()
        val done = AtomicBoolean()

        runBlocking {
            val execution =
                launch {
                    done.set(mock.execute() == "first")
                }

            val thread =
                thread {
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
        val mock =
            mockk<Executable> {
                coEvery { execute() } coAnswers { task() }
            }

        val timeout = AtomicBoolean()

        runBlocking {
            val execution =
                launch {
                    mock.execute()
                } // This blocks the test indefinitely
            val thread =
                thread {
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
        val mock =
            mockk<Executable> {
                coEvery { execute() } coAnswers { task() } andThenThrows RuntimeException("test")
            }

        val timeout = AtomicBoolean()
        val done = AtomicBoolean()

        runBlocking {
            val execution =
                launch {
                    if (mock.execute() != "first") {
                        return@launch
                    }

                    try {
                        mock.execute()
                    } catch (ex: RuntimeException) {
                        done.set(true)
                    }
                }

            val thread =
                thread {
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
        val mock =
            mockk<Executable> {
                coEvery { execute() } coAnswers { task1() } coAndThen { task2() }
            }

        val timeout = AtomicBoolean()
        val done = AtomicBoolean()

        runBlocking {
            val execution =
                launch {
                    done.set(
                        mock.execute() == "first" &&
                            mock.execute() == "second",
                    )
                }

            val thread =
                thread {
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

    fun interface SuspendingUseCase {
        suspend operator fun invoke(event: String)
    }

    /**
     * GitHub issue #1555: kotlin-reflect can misreport a functional (SAM) interface's suspend
     * member as non-suspend; `coVerify` must still treat the trailing `Continuation` as `any()`.
     */
    @Test
    fun coVerifyOnSpykOfSuspendFunctionalInterface() {
        val mock = spyk(SuspendingUseCase {})

        runBlocking {
            mock("hello")
            coVerify { mock("hello") }
        }
    }

    class SuspendingCls {
        suspend fun suspendOnce(): Int {
            delay(1)
            return 1
        }

        suspend fun countDownOnDefaultDispatcher(n: Int): Int =
            if (n == 0) 0 else withContext(Dispatchers.Default) { countDownOnDefaultDispatcher(n - 1) } + 1
    }

    object SuspendingObj {
        suspend fun suspendOnce(): Int {
            delay(1)
            return 1
        }
    }

    /**
     * GitHub issue #554: a suspend function resumes by calling itself again with its own
     * continuation, which must not be recorded as a second call.
     */
    @Test
    fun spykRecordsOneCallForSuspendFunctionThatSuspends() {
        val spy = spyk(SuspendingCls())

        runBlocking {
            assertEquals(1, spy.suspendOnce())
        }

        coVerify(exactly = 1) { spy.suspendOnce() }
    }

    @Test
    fun spykRecordsOneCallForSuspendFunctionThatSuspendsInLaunchedCoroutine() {
        val spy = spyk(SuspendingCls())

        runBlocking {
            launch { spy.suspendOnce() }.join()
        }

        coVerify(exactly = 1) { spy.suspendOnce() }
    }

    @Test
    fun spykRecordsCallsMadeFromSuspendLambdaInsideTheSameFunction() {
        val spy = spyk(SuspendingCls())

        runBlocking {
            assertEquals(1, spy.countDownOnDefaultDispatcher(1))
        }

        coVerifySequence {
            spy.countDownOnDefaultDispatcher(1)
            spy.countDownOnDefaultDispatcher(0)
        }
    }

    @Test
    fun mockkObjectRecordsOneCallForSuspendFunctionThatSuspends() {
        mockkObject(SuspendingObj)
        try {
            runBlocking {
                assertEquals(1, SuspendingObj.suspendOnce())
            }

            coVerify(exactly = 1) { SuspendingObj.suspendOnce() }
        } finally {
            unmockkObject(SuspendingObj)
        }
    }
}
