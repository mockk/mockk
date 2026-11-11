package io.mockk.it

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Regression tests for https://github.com/mockk/mockk/issues/1398:
 * "MockKException: Bad recording sequence. State: AnsweringState when using coVerify and coAnswers".
 *
 * Under kotest 6 concurrent test execution several test coroutines interleave on one
 * thread, including inside the `runBlocking` window that executes a `coEvery`/`coVerify`
 * block. A second test's recording session then starts on the same thread-local recorder
 * while the first test's session is in the middle of its block, and the first session is
 * suspended until the second one is finished. When the nested session FAILS (for example
 * a `coVerify` count check), its cleanup used to drop the suspended outer session as
 * well, so the outer block resumed recording against the idle AnsweringState and failed
 * with "Bad recording sequence. State: AnsweringState" instead of the outer stubbing
 * simply continuing.
 */
class Issue1398ConcurrentRecordingSessionTest {
    class TrustService {
        @Suppress("unused")
        suspend fun getEmailVerificationSignal(signal: String): String = signal
    }

    @Test
    fun `failing nested verification inside the co-block window does not corrupt the outer session`() {
        val trustService =
            mockk<TrustService>(relaxed = false)

        val nestedError = CompletableDeferred<Throwable>()

        val outerAnswer: String =
            runBlocking {
                coEvery {
                    // Simulate a second test coroutine interleaving on this thread while
                    // this block's recording session is in progress: kotest 6 runs test
                    // coroutines concurrently and they can resume inside the runBlocking
                    // window that executes this block.
                    val nested =
                        CoroutineScope(coroutineContext).launch {
                            try {
                                coVerify(exactly = 1) { trustService.getEmailVerificationSignal(any()) }
                            } catch (expected: Throwable) {
                                // like kotest's property-test machinery, the failing
                                // iteration contains its own error and keeps running
                                nestedError.complete(expected)
                            }
                        }
                    nested.join()

                    trustService.getEmailVerificationSignal("a@b.c")
                } coAnswers { call ->
                    "verified: ${call.invocation.args[0]}"
                }
                val answer = trustService.getEmailVerificationSignal("a@b.c")

                // the nested verification still reports its own failure
                assertTrue(nestedError.await().message!!.contains("was not called"))
                answer
            }

        assertEquals("verified: a@b.c", outerAnswer)
    }

    @Test
    fun `succeeding nested verification inside the co-block window resumes the outer session`() {
        val trustService =
            mockk<TrustService>(relaxed = false)

        val outerAnswer: String =
            runBlocking {
                coEvery {
                    val nested =
                        CoroutineScope(coroutineContext).launch {
                            // nothing was actually called yet: verifying exactly = 0 succeeds
                            coVerify(exactly = 0) { trustService.getEmailVerificationSignal(any()) }
                        }
                    nested.join()

                    trustService.getEmailVerificationSignal("a@b.c")
                } coAnswers { call ->
                    "verified: ${call.invocation.args[0]}"
                }
                trustService.getEmailVerificationSignal("a@b.c")
            }

        assertEquals("verified: a@b.c", outerAnswer)
    }

    @Test
    fun `a failing verification still reports its own error`() {
        val trustService =
            mockk<TrustService>(relaxed = false)

        val error =
            assertFailsWith<AssertionError> {
                runBlocking {
                    coVerify(exactly = 2) { trustService.getEmailVerificationSignal(any()) }
                }
            }

        assertTrue(
            !error.message!!.contains("Bad recording sequence"),
            "unexpected error: ${error.message}",
        )
    }

    @Test
    fun `recorder remains usable for later sessions after a contained nested failure`() {
        val trustService =
            mockk<TrustService>(relaxed = false)

        val nestedError = CompletableDeferred<Throwable>()

        runBlocking {
            coEvery {
                val nested =
                    CoroutineScope(coroutineContext).launch {
                        try {
                            coVerify(exactly = 1) { trustService.getEmailVerificationSignal(any()) }
                        } catch (expected: Throwable) {
                            nestedError.complete(expected)
                        }
                    }
                nested.join()

                trustService.getEmailVerificationSignal("a@b.c")
            } coAnswers { call ->
                "first: ${call.invocation.args[0]}"
            }

            // a subsequent session on the same recorder must not be affected
            coEvery { trustService.getEmailVerificationSignal(any()) } coAnswers { call ->
                "second: ${call.invocation.args[0]}"
            }
            assertEquals("second: a@b.c", trustService.getEmailVerificationSignal("a@b.c"))

            // the nested verification still reports its own failure
            assertTrue(nestedError.await().message!!.contains("was not called"))
        }
    }
}
