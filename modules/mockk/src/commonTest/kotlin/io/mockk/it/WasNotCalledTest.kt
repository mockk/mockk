package io.mockk.it

import io.mockk.Called
import io.mockk.MockKException
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WasNotCalledTest {
    val mock = mockk<MockCls>()

    @Test
    fun wasNotCalledOnMockedObject() {
        verify { mock wasNot Called }
    }

    @Test
    fun wasNotCalledShouldThrowAssertionErrorIfMockHasBeenCalled() {
        assertFailsWith<AssertionError>("was not can should throw AssertionError if mock has been called") {
            every { mock.flowOp(1, 2) } returns flowOf(1, 2)

            mock.flowOp(1, 2)
            verify { mock wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnPlainValueStillThrows() {
        assertFailsWith<MockKException>("was not can should throw MockKException on non mock object") {
            verify { "a plain value" wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnMethodCallResult() {
        verify { mock.flowOp(1, 2) wasNot Called }
    }

    @Test
    fun wasNotCalledOnMethodCallResultShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        every { mock.flowOp(1, 2) } returns flowOf(1, 2)

        mock.flowOp(1, 2)

        assertFailsWith<AssertionError> {
            verify { mock.flowOp(1, 2) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnMethodCallResultWithAnyMatcher() {
        verify { mock.flowOp(any(), any()) wasNot Called }
    }

    @Test
    fun wasNotCalledOnMethodCallResultWithAnyMatcherShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        every { mock.flowOp(any(), any()) } returns flowOf(1, 2)

        mock.flowOp(1, 2)

        assertFailsWith<AssertionError> {
            verify { mock.flowOp(any(), any()) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnStringReturningMethodCallResult() {
        verify { mock.stringOp(1) wasNot Called }
    }

    @Test
    fun wasNotCalledOnStringReturningMethodCallResultShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        every { mock.stringOp(1) } returns "stubbed"

        mock.stringOp(1)

        assertFailsWith<AssertionError> {
            verify { mock.stringOp(1) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnUnitReturningMethodCallResult() {
        verify { mock.unitOp(1) wasNot Called }
    }

    @Test
    fun wasNotCalledOnUnitReturningMethodCallResultShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        every { mock.unitOp(1) } just Runs

        mock.unitOp(1)

        assertFailsWith<AssertionError> {
            verify { mock.unitOp(1) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnSuspendMethodCallResult() {
        coVerify { mock.suspendOp(1) wasNot Called }
    }

    @Test
    fun wasNotCalledOnSuspendMethodCallResultShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        runBlocking {
            coEvery { mock.suspendOp(1) } returns "stubbed"

            mock.suspendOp(1)

            assertFailsWith<AssertionError> {
                coVerify { mock.suspendOp(1) wasNot Called }
            }
        }
    }

    @Test
    fun wasNotCalledOnMethodCallResultDoesNotDisableOtherVerifications() {
        every { mock.stringOp(1) } returns "stubbed"
        every { mock.flowOp(1, 2) } returns flowOf(1, 2)

        mock.stringOp(1)
        mock.flowOp(1, 2)

        assertFailsWith<AssertionError> {
            verify {
                mock.stringOp(1)
                mock.flowOp(1, 2) wasNot Called
            }
        }

        verify(exactly = 1) { mock.stringOp(1) }
        verify(exactly = 1) { mock.flowOp(any(), any()) }
    }

    @Test
    fun wasNotCalledOnMockedObjectTogetherWithMethodCallResult() {
        verify {
            mock.flowOp(1, 2) wasNot Called
            mock wasNot Called
        }
    }

    @Test
    fun wasNotCalledOnMethodCallResultInSequenceVerification() {
        verifySequence {
            mock.flowOp(1, 2) wasNot Called
        }
    }

    @Test
    fun wasNotCalledOnSuperclassMethodCallResult() {
        val child = mockk<DerivedCls>()

        every { child.superOp(5) } returns "stubbed"

        verify { child.superOp(5) wasNot Called }
    }

    @Test
    fun wasNotCalledOnSuperclassMethodCallResultWithoutStub() {
        val child = mockk<DerivedCls>()

        verify { child.superOp(5) wasNot Called }
    }

    @Test
    fun wasNotCalledOnSuperclassMethodCallResultShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        val child = mockk<DerivedCls>()

        every { child.superOp(5) } returns "stubbed"
        child.superOp(5)

        assertFailsWith<AssertionError> {
            verify { child.superOp(5) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnOwnMethodCallResultOfSubclass() {
        val child = mockk<DerivedCls>()

        every { child.ownOp(5) } returns "stubbed"

        verify { child.ownOp(5) wasNot Called }
    }

    @Test
    fun wasNotCalledOnOwnMethodCallResultOfSubclassShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        val child = mockk<DerivedCls>()

        every { child.ownOp(5) } returns "stubbed"
        child.ownOp(5)

        assertFailsWith<AssertionError> {
            verify { child.ownOp(5) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnInterfaceMethodCallResult() {
        val repo = mockk<Repository>()

        every { repo.countBy(any()) } returns emptyMap()

        verify { repo.countBy(any()) wasNot Called }
    }

    @Test
    fun wasNotCalledOnInterfaceMethodCallResultShouldThrowAssertionErrorIfMethodHasBeenCalled() {
        val repo = mockk<Repository>()

        every { repo.countBy(any()) } returns emptyMap()
        repo.countBy(setOf(1L))

        assertFailsWith<AssertionError> {
            verify { repo.countBy(any()) wasNot Called }
        }
    }

    @Test
    fun wasNotCalledOnChainedMethodCallResult() {
        val relaxed = mockk<DerivedCls>(relaxed = true)

        verify { relaxed.derivedOp().nestedOp() wasNot Called }
    }

    @Test
    fun wasNotCalledOnChainedMethodCallResultShouldThrowAssertionErrorIfCalled() {
        val relaxed = mockk<DerivedCls>(relaxed = true)

        relaxed.derivedOp().nestedOp()

        assertFailsWith<AssertionError> {
            verify { relaxed.derivedOp().nestedOp() wasNot Called }
        }
    }

    class MockCls {
        fun flowOp(
            a: Int = 1,
            b: Int = 2,
        ) = flowOf(a, b)

        fun stringOp(a: Int): String = "value:$a"

        fun unitOp(a: Int) {
            // no-op
        }

        suspend fun suspendOp(a: Int): String = "value:$a"
    }

    open class BaseCls {
        open fun superOp(x: Int): String = "super:$x"
    }

    class DerivedCls : BaseCls() {
        fun ownOp(x: Int): String = "own:$x"

        fun derivedOp(): NestedCls = NestedCls()

        class NestedCls {
            fun nestedOp(): String = "nested"
        }
    }

    interface Repository {
        fun countBy(ids: Set<Long>): Map<Long, Long>
    }
}
