package io.mockk.it

import io.mockk.InternalPlatformDsl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Issue #1362: a recorded block of `every`/`verify` (and their `co` variants) may be evaluated
 * several times: each round generates fresh random signature values for the registered matchers,
 * which lets the signature matcher detector tie a matcher to its call argument.
 *
 * A block that registers no matchers has nothing to tie, so it is evaluated exactly once and
 * side effects placed inside the block (logging, counting, ...) happen once as well.
 */
class RecordedBlockRunsTest {
    class MockCls {
        fun op(
            flag: Boolean,
            value: Int,
        ): String = "op"

        suspend fun coOp(
            flag: Boolean,
            value: Int,
        ): String = "coOp"
    }

    @Test
    fun verifyBlockWithoutMatchersIsEvaluatedOnce() {
        val mock = mockk<MockCls>()
        every { mock.op(any(), any()) } returns "stub"
        mock.op(true, 1)

        var evaluations = 0
        verify {
            evaluations++
            mock.op(true, 1)
        }

        assertEquals(1, evaluations)
    }

    @Test
    fun coVerifyBlockWithoutMatchersIsEvaluatedOnce() {
        val mock = mockk<MockCls>()
        coEvery { mock.coOp(flag = true, value = 1) } returns "stub"
        InternalPlatformDsl.runCoroutine {
            assertEquals("stub", mock.coOp(flag = true, value = 1))
        }

        var evaluations = 0
        coVerify {
            evaluations++
            mock.coOp(flag = true, value = 1)
        }

        assertEquals(1, evaluations)
    }

    @Test
    fun everyBlockWithoutMatchersIsEvaluatedOnce() {
        val mock = mockk<MockCls>()

        var evaluations = 0
        every {
            evaluations++
            mock.op(flag = true, value = 1)
        } returns "stub"

        assertEquals(1, evaluations)
        assertEquals("stub", mock.op(true, 1))
    }

    @Test
    fun verifyBlockWithoutMatchersStillDetectsArgumentMismatch() {
        val mock = mockk<MockCls>()
        every { mock.op(any(), any()) } returns "stub"
        mock.op(true, 1)

        var evaluations = 0
        assertFailsWith<AssertionError> {
            verify {
                evaluations++
                mock.op(false, 1)
            }
        }

        assertEquals(1, evaluations)
    }

    @Test
    fun verifyBlockWithMatchersStillMatchesBooleanArguments() {
        val mock = mockk<MockCls>()
        every { mock.op(any(), any()) } returns "stub"
        mock.op(true, 1)
        mock.op(false, 2)

        verify {
            mock.op(any(), 1)
        }
        verify(exactly = 1) {
            mock.op(eq(false), any())
        }
    }
}
