package io.mockk.it

import io.mockk.Called
import io.mockk.MockKException
import io.mockk.clearConstructorMockk
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Verification of a bare `anyConstructed<T>()` placeholder.
 *
 * The placeholder records no call of its own, so it is verified as an aggregate target:
 * `verify(exactly = 0) { anyConstructed<T>() }` passes while no constructed instance of
 * `T` was used and fails reporting the recorded calls otherwise.
 */
class ConstructorMockVerificationTest {
    class MockCls(
        private val x: Int = 0,
    ) {
        fun op(
            a: Int,
            b: Int,
        ) = a + b + x
    }

    class OtherCls {
        fun execute() = Unit
    }

    class RegularCls {
        fun op(
            a: Int,
            b: Int,
        ) = a + b
    }

    @Test
    fun zeroCallsPassesWhenNothingWasConstructed() {
        mockkConstructor(MockCls::class)

        verify(exactly = 0) { anyConstructed<MockCls>() }
    }

    @Test
    fun zeroCallsPassesWhenNothingWasCalledAfterStubbing() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        verify(exactly = 0) { anyConstructed<MockCls>() }
    }

    @Test
    fun zeroCallsPassesWhenConstructedInstanceWasNeverCalled() {
        mockkConstructor(MockCls::class)

        MockCls()

        verify(exactly = 0) { anyConstructed<MockCls>() }
    }

    @Test
    fun zeroCallsPassesAfterConstructorMockWasCleared() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4
        MockCls().op(1, 2)

        clearConstructorMockk(MockCls::class)

        verify(exactly = 0) { anyConstructed<MockCls>() }
    }

    @Test
    fun zeroCallsFailsWhenConstructedInstanceWasCalled() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4
        MockCls().op(1, 2)

        val error =
            assertFailsWith<AssertionError> {
                verify(exactly = 0) { anyConstructed<MockCls>() }
            }
        val message = error.message ?: return

        assertTrue("constructed instances" in message)
        assertTrue("op(" in message)
    }

    @Test
    fun zeroCallsFailsWhenConstructionsHappenedBeforeAnyPlaceholderUsage() {
        mockkConstructor(MockCls::class)

        // No `anyConstructed<MockCls>()` was ever used before the construction, so the
        // constructed instance is connected to its own representative mock. The aggregate
        // verification still has to see calls recorded through that representative.
        MockCls().op(1, 2)

        val error =
            assertFailsWith<AssertionError> {
                verify(exactly = 0) { anyConstructed<MockCls>() }
            }
        val message = error.message ?: return

        assertTrue("constructed instances" in message)
    }

    @Test
    fun zeroCallsChecksEachRequestedPlaceholderType() {
        mockkConstructor(MockCls::class)
        mockkConstructor(OtherCls::class)

        verify(exactly = 0) {
            anyConstructed<MockCls>()
            anyConstructed<OtherCls>()
        }

        OtherCls().execute()

        assertFailsWith<AssertionError> {
            verify(exactly = 0) {
                anyConstructed<MockCls>()
                anyConstructed<OtherCls>()
            }
        }
    }

    @Test
    fun barePlaceholderWithOtherBoundsStillReportsMissingCalls() {
        mockkConstructor(MockCls::class)

        val error =
            assertFailsWith<MockKException> {
                verify(exactly = 1) { anyConstructed<MockCls>() }
            }
        val message = error.message ?: return

        assertTrue("Missing calls inside verify { ... } block." in message)
        assertTrue("exactly = 0" in message)
    }

    @Test
    fun wasNotCalledKeepsItsSemantics() {
        mockkConstructor(MockCls::class)

        // A concrete constructed instance supports `wasNot Called` the same way as before
        // the aggregate `exactly = 0` verification existed.
        val instance = MockCls()

        verify { instance wasNot Called }

        every { anyConstructed<MockCls>().op(1, 2) } returns 4
        instance.op(1, 2)

        assertFailsWith<AssertionError> {
            verify { instance wasNot Called }
        }
    }

    @Test
    fun barePlaceholderWasNotCalledIsUnchanged() {
        mockkConstructor(MockCls::class)

        // The bare placeholder itself is not accepted by `wasNot Called`. That is
        // unchanged pre-existing behavior, independent of the aggregate `exactly = 0`
        // verification introduced here.
        assertFailsWith<MockKException> {
            verify { anyConstructed<MockCls>() wasNot Called }
        }
    }

    @Test
    fun callVerificationOnPlaceholderIsUnchanged() {
        mockkConstructor(MockCls::class)

        every { anyConstructed<MockCls>().op(1, 2) } returns 4

        verify(exactly = 0) { anyConstructed<MockCls>().op(1, 2) }

        MockCls().op(1, 2)

        verify { anyConstructed<MockCls>().op(1, 2) }

        assertFailsWith<AssertionError> {
            verify(exactly = 0) { anyConstructed<MockCls>().op(1, 2) }
        }
    }

    @Test
    fun aggregateZeroCallsDoesNotAffectRegularMockVerification() {
        mockkConstructor(MockCls::class)

        val regular = mockk<RegularCls>()

        verify(exactly = 0) { anyConstructed<MockCls>() }

        every { regular.op(any(), any()) } returns 1
        regular.op(1, 2)

        verify { regular.op(1, 2) }
        verify(exactly = 0) { anyConstructed<MockCls>() }
    }
}
