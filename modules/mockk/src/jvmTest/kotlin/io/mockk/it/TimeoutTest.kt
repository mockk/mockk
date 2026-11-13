package io.mockk.it

import io.mockk.Called
import io.mockk.Ordering
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class TimeoutTest {
    class MockCls {
        fun run() {
        }

        fun stop() {
        }

        fun take(value: Int) {
        }

        fun record(
            first: Int,
            second: String,
        ) {
        }
    }

    @RelaxedMockK
    lateinit var mock1: MockCls

    @RelaxedMockK
    lateinit var mock2: MockCls

    @Test
    fun timeoutOnNeverCalled() {
        assertFails {
            verify(timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun timeoutOnCalledLater() {
        launch {
            delay(1000)
            mock1.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun okIfCalledEarlier() {
        launch {
            delay(200)
            mock1.run()
        }

        verify(timeout = 500) {
            mock1.run()
        }
    }

    @Test
    fun failIfOneMockCalled() {
        launch {
            delay(200)
            mock1.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1.run()
                mock2.run()
            }
        }
    }

    @Test
    fun failIfOneMockCalledAndOneLater() {
        launch {
            delay(200)
            mock1.run()
            delay(1000)
            mock2.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1.run()
                mock2.run()
            }
        }
    }

    @Test
    fun okIfBothInTime() {
        launch {
            delay(100)
            mock1.run()
            delay(100)
            mock2.run()
        }

        verify(timeout = 500) {
            mock1.run()
            mock2.run()
        }
    }

    @Test
    fun longTimeout() {
        launch {
            delay(500)
            mock1.run()
        }

        val start = System.currentTimeMillis()
        verify(timeout = 5000) {
            mock1.run()
        }
        val end = System.currentTimeMillis()
        val duration = end - start

        assertTrue(duration < 1500, "waiting too long")
    }

    @Test
    fun wasNotCalledFailsIfCalledBeforeTimeout() {
        launch {
            delay(200)
            mock1.run()
        }

        assertFails {
            verify(timeout = 500) {
                mock1 wasNot Called
            }
        }
    }

    @Test
    fun wasNotCalledOkIfCalledAfterTimeout() {
        launch {
            delay(1000)
            mock1.run()
        }

        verify(timeout = 500) {
            mock1 wasNot Called
        }
    }

    @Test
    fun exactlyZeroFailsIfCalledBeforeTimeout() {
        launch {
            delay(200)
            mock1.run()
        }

        assertFails {
            verify(exactly = 0, timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun exactlyZeroOkIfCalledAfterTimeout() {
        launch {
            delay(1000)
            mock1.run()
        }

        verify(exactly = 0, timeout = 500) {
            mock1.run()
        }
    }

    @Test
    fun inverseFailsIfCalledBeforeTimeout() {
        launch {
            delay(200)
            mock1.run()
        }

        assertFails {
            verify(inverse = true, timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun coVerifyExactlyOnceFailsIfCalledAgainBeforeTimeout() {
        launch {
            delay(100)
            mock1.run()
            delay(200)
            mock1.run()
        }

        assertFails {
            coVerify(exactly = 1, timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun sequenceFailsIfAnotherCallBeforeTimeout() {
        launch {
            delay(100)
            mock1.run()
            delay(200)
            mock1.stop()
        }

        assertFails {
            verify(ordering = Ordering.SEQUENCE, timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun sequenceOkIfAnotherCallAfterTimeout() {
        launch {
            delay(100)
            mock1.run()
            delay(900)
            mock1.stop()
        }

        verify(ordering = Ordering.SEQUENCE, timeout = 500) {
            mock1.run()
        }
    }

    @Test
    fun allFailsIfUnmatchedCallBeforeTimeout() {
        launch {
            delay(100)
            mock1.run()
            delay(200)
            mock1.stop()
        }

        assertFails {
            verify(ordering = Ordering.ALL, timeout = 500) {
                mock1.run()
            }
        }
    }

    @Test
    fun waitsForTheNextCallAfterAnUnmatchedOne() {
        var checks = 0
        launch {
            delay(100)
            mock1.take(1)
        }

        assertFails {
            verify(timeout = 500) {
                mock1.take(
                    match {
                        checks++
                        it == 2
                    },
                )
            }
        }
        assertTrue(checks < 10, "matcher was checked $checks times")
    }

    @Test
    fun exactlyFailsCleanlyWithStandardReportIfNeverCalled() {
        val ex =
            assertFailsWith<AssertionError> {
                verify(exactly = 3, timeout = 500) {
                    mock1.run()
                }
            }

        assertTrue(
            ex.message!!.contains("was not called"),
            "should fail with the standard verification report, got: " + ex.message,
        )
    }

    @Test
    fun exactlyOkIfCalledThreeTimesWithinTimeout() {
        launch {
            delay(100)
            mock1.run()
            mock1.run()
            mock1.run()
        }

        verify(exactly = 3, timeout = 500) {
            mock1.run()
        }
    }

    @Test
    fun concurrentRecordingDuringTimeoutVerificationFailsCleanly() {
        // issue #1221: with calls being recorded by another thread while the timeout
        // verification polls, the failure report paths crashed with NoSuchElementException
        // on an empty recorded-calls list instead of failing the verification normally.
        launch {
            repeat(600) {
                mock1.take(it)
                delay(1)
            }
        }

        val ex =
            assertFailsWith<AssertionError> {
                verify(timeout = 500) {
                    mock1.take(match { it < 0 })
                }
            }

        val message = ex.message!!
        assertTrue(
            message.contains("was not called") || message.contains("No matching calls found"),
            "should fail with the standard verification report, got: " + message,
        )
    }

    private fun launch(block: () -> Unit) {
        Thread(block).start()
    }

    private fun delay(ms: Long) = Thread.sleep(ms)

    // ---- issue #1169: verify(timeout) passing while answer-side capture slots are unfilled ----
    //
    // A slot captured in `every { ... capture(slot) }` is filled by the answering thread inside
    // stub.answer(...). Before the fix, the call was recorded (and timeout waiters were notified)
    // before that capture ran, so verify(timeout) could pass while the slot was still empty.

    @Test
    fun captureSlotIsFilledBeforeTimeoutVerificationPasses() {
        val slot = slot<Int>()
        every {
            mock1.record(
                capture(slot),
                match {
                    // Widens the window between recording the call and filling its capture
                    // slots: the answer matcher runs right after the call is visible to the
                    // verification, so a verification woken by the recording passes while this
                    // matcher is still running and the slot is not yet filled.
                    Thread.sleep(200)
                    true
                },
            )
        } just runs

        launch {
            delay(100) // let the verification park in the record-call await session first
            mock1.record(7, "seven")
        }

        verify(timeout = 10_000) { mock1.record(any(), any()) }
        assertEquals(7, slot.captured)
    }

    @Test
    fun captureSlotIsFilledEveryTimeTheCallLandsWithinTheWindow() {
        repeat(30) { i ->
            clearMocks(mock1)
            val slot = slot<Int>()
            every { mock1.take(capture(slot)) } just runs

            launch {
                delay(10)
                mock1.take(i)
            }

            verify(timeout = 5_000) { mock1.take(any()) }
            assertTrue(slot.isCaptured, "iteration $i: slot was not captured when verification passed")
            assertEquals(i, slot.captured, "iteration $i: unexpected captured value")
        }
    }

    @Test
    fun captureListIsFilledWhenTheCallLandsWithinTheWindow() {
        repeat(10) { i ->
            clearMocks(mock1)
            val list = mutableListOf<Int>()
            every { mock1.take(capture(list)) } just runs

            launch {
                delay(10)
                mock1.take(i)
            }

            verify(timeout = 5_000) { mock1.take(any()) }
            assertEquals(listOf(i), list, "iteration $i: captured list does not hold the call argument")
        }
    }

    @Test
    fun captureSlotStaysEmptyIfCallNeverLandsWithinTimeout() {
        val slot = slot<Int>()
        every { mock1.take(capture(slot)) } just runs

        assertFails {
            verify(timeout = 300) { mock1.take(any()) }
        }
        assertFalse(slot.isCaptured, "slot must stay empty when the call never lands")
    }

    @Test
    fun verifyBlockCaptureSlotIsFilledWhenCallLandsWithinTimeout() {
        val slot = slot<Int>()

        launch {
            delay(100)
            mock1.take(5)
        }

        verify(timeout = 5_000) { mock1.take(capture(slot)) }
        assertEquals(5, slot.captured)
    }

    @Test
    fun captureSlotIsFilledWhenCoroutineCallsWithinTimeout() {
        runBlocking {
            val slot = slot<Int>()
            every { mock1.take(capture(slot)) } just runs

            launch(Dispatchers.IO) {
                delay(50) // blocking helper, a real worker thread parks here
                mock1.take(42)
            }

            verify(timeout = 5_000) { mock1.take(any()) }
            assertEquals(42, slot.captured)
        }
    }
}
