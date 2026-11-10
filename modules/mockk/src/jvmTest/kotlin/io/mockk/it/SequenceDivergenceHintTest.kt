package io.mockk.it

import io.mockk.MockKException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * Ordered/sequence verification failures get an additive note pointing at the call where the
 * recorded flow most likely diverged: a call that reached the mock but had no stubbed answer
 * or threw during execution aborted the code under test, which is why the later calls never
 * happened (issue #959). The note is messaging only, so the original failure content is kept.
 */
class SequenceDivergenceHintTest {
    open class SeqCls {
        fun op(
            a: Int,
            b: Int,
        ): Int = a + b
    }

    @Test
    fun sequenceWithUnstubbedCallHintsAtTheDivergencePoint() {
        val mock = mockk<SeqCls>()
        every { mock.op(1, 0) } returns 10

        mock.op(1, 0)
        try {
            // never stubbed: throws MockKException('no answer found'), the code under test
            // swallows it and skips the remaining calls
            mock.op(2, 0)
        } catch (_: MockKException) {
        }

        val failure =
            assertFailsWith<AssertionError> {
                verifySequence {
                    mock.op(1, 0)
                    mock.op(2, 0)
                    mock.op(3, 0)
                }
            }
        val message = failure.message!!

        assertContains(message, "number of calls happened not matching exact number of verification sequence")
        assertContains(message, "had no stubbed answer / threw during execution")
        assertContains(message, "this is likely where the sequence diverged")
        assertContains(message, "op(eq(2), eq(0))")
    }

    @Test
    fun sequenceWithCallThatThrewDuringExecutionHintsAtIt() {
        val mock = mockk<SeqCls>()
        every { mock.op(1, 0) } returns 10
        every { mock.op(2, 0) } throws IllegalStateException("boom")

        mock.op(1, 0)
        try {
            mock.op(2, 0)
        } catch (_: IllegalStateException) {
        }

        val failure =
            assertFailsWith<AssertionError> {
                verifySequence {
                    mock.op(1, 0)
                    mock.op(2, 0)
                    mock.op(3, 0)
                }
            }
        val message = failure.message!!

        assertContains(message, "number of calls happened not matching exact number of verification sequence")
        assertContains(message, "had no stubbed answer / threw during execution")
        assertContains(message, "op(eq(2), eq(0))")
    }

    @Test
    fun fullyStubbedSequenceWithArgumentMismatchKeepsMessageWithoutHint() {
        val mock = mockk<SeqCls>()
        every { mock.op(1, 0) } returns 10
        every { mock.op(2, 0) } returns 20

        mock.op(1, 0)
        mock.op(2, 0)

        val failure =
            assertFailsWith<AssertionError> {
                verifySequence {
                    mock.op(1, 0)
                    mock.op(9, 9)
                }
            }
        val message = failure.message!!

        assertContains(message, "calls are not exactly matching verification sequence")
        assertFalse(message.contains("had no stubbed answer"), "Message: $message")
    }

    @Test
    fun orderVerificationWithUnstubbedCallAlsoHintsAtTheDivergencePoint() {
        val mock = mockk<SeqCls>()
        every { mock.op(1, 0) } returns 10

        mock.op(1, 0)
        try {
            mock.op(2, 0)
        } catch (_: MockKException) {
        }

        val failure =
            assertFailsWith<AssertionError> {
                verifyOrder {
                    mock.op(1, 0)
                    mock.op(2, 0)
                    mock.op(3, 0)
                }
            }
        val message = failure.message!!

        assertContains(message, "fewer calls happened than demanded by order verification sequence")
        assertContains(message, "had no stubbed answer / threw during execution")
    }

    @Test
    fun fullyStubbedSequenceStillVerifiesAndCapturesAsBefore() {
        val mock = mockk<SeqCls>()
        every { mock.op(1, 0) } returns 10
        every { mock.op(2, 0) } returns 20

        mock.op(1, 0)
        mock.op(2, 0)

        val capturedB = slot<Int>()
        verifySequence {
            mock.op(1, 0)
            mock.op(2, capture(capturedB))
        }

        assertEquals(0, capturedB.captured)
    }

    @Test
    fun spyUnstubbedCallAnsweredByOriginalDoesNotHint() {
        val spy = spyk<SeqCls>()
        every { spy.op(1, 0) } returns 10

        spy.op(1, 0)
        spy.op(2, 0) // unstubbed, but the spy falls through to the original method and answers fine

        val failure =
            assertFailsWith<AssertionError> {
                verifySequence {
                    spy.op(1, 0)
                    spy.op(2, 0)
                    spy.op(3, 0)
                }
            }
        val message = failure.message!!

        assertContains(message, "number of calls happened not matching exact number of verification sequence")
        assertFalse(message.contains("had no stubbed answer"), "Message: $message")
    }
}
