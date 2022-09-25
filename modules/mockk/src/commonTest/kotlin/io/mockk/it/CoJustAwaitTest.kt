package io.mockk.it

import io.mockk.andThenJust
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coJustAwait
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoJustAwaitTest {

    private val mock = mockk<MockCls>()

    @Test
    fun `coJustRun completes as expected`() = runTest {
        coJustRun { mock.coOp() }

        val job = launch { mock.coOp() }
        runCurrent()

        coVerify(exactly = 1) { mock.coOp() }
        verify(exactly = 0) { mock.notImplemented() }
        confirmVerified(mock)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `coJustAwait awaits until cancellation`() = runTest {
        coJustAwait { mock.coOp() }

        val job = launch { mock.coOp() }
        runCurrent()

        coVerify(exactly = 1) { mock.coOp() }
        verify(exactly = 0) { mock.notImplemented() }
        confirmVerified(mock)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    @Test
    fun `coJustAwait andThenJust answers and awaits until cancellation`() = runTest {
        coEvery { mock.coOp(any()) } answers { 1 } andThenJust awaits

        val job = launch {
            repeat(2) { mock.coOp(it) }
        }
        runCurrent()

        coVerifySequence {
            mock.coOp(0)
            mock.coOp(1)
        }
        verify(exactly = 0) { mock.notImplemented() }
        confirmVerified(mock)
        assertTrue(job.isActive)
        job.cancelAndJoin()
    }

    class MockCls {
        @Suppress("RedundantSuspendModifier")
        suspend fun coOp(a: Int = 1): Int = a + notImplemented()
        fun notImplemented(): Int = TODO()
    }

}

