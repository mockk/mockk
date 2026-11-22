package io.mockk.it

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A recorded block that calls a function whose result is `kotlin.Nothing` ends with a
 * compiler-inserted `KotlinNothingValueException` - the call itself is recorded, but the block
 * never completes normally. MockK has to treat that exception as the end of the block instead
 * of failing the whole session (#960).
 *
 * The type can reach the call in two ways: declared on the function itself (where the
 * pre-existing `isLastCallReturnsNothing` machinery already coped, as the control below shows)
 * or via a type argument such as `() -> Nothing`, invisible in the function's own signature
 * (`Function0.invoke` returns the type variable `R`), which is the failing case.
 */
class NothingReturningCallRecordingTest {
    interface NothingReturning {
        fun stop(): Nothing
    }

    @Suppress("UNUSED_PARAMETER")
    fun consume(callback: () -> Nothing) {
        callback()
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun consumeSuspending(callback: suspend () -> Nothing) {
        callback()
    }

    @Test
    fun `stubbing a Nothing-typed lambda call configures the thrown answer`() {
        val mockCallback = mockk<() -> Nothing>()

        every { mockCallback() } throws IllegalStateException("mock exception")

        val thrown = assertThrows<IllegalStateException> { consume(mockCallback) }
        assertEquals("mock exception", thrown.message)

        verify { mockCallback() }
    }

    @Test
    fun `suspending Nothing-typed lambda call is stubbed and verified`() =
        runTest {
            val mockCallback = mockk<suspend () -> Nothing>()
            val counter = AtomicInteger()

            coEvery { mockCallback() } answers {
                counter.incrementAndGet()
                throw IllegalStateException("co mock exception")
            }

            assertThrows<IllegalStateException> { consumeSuspending(mockCallback) }
            assertEquals(1, counter.get())

            coVerify { mockCallback() }
        }

    /**
     * Control: a `Nothing` return type declared on the function itself goes through the
     * pre-existing last-call-returns-Nothing handling and already passes on unpatched master.
     */
    @Test
    fun `control - declared Nothing return type already works`() {
        val mock = mockk<NothingReturning>()

        every { mock.stop() } throws IllegalStateException("declared nothing exception")

        assertThrows<IllegalStateException> { mock.stop() }

        verify { mock.stop() }
    }
}
