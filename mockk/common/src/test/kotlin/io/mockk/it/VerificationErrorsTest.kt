package io.mockk.it

import io.mockk.*
import io.mockk.test.SkipInstrumentedAndroidTest
import kotlin.test.Test
import kotlin.test.fail

class VerificationErrorsTest {
    val mock = mockk<MockCls>("mock")
    val openMock = mockk<OpenMockCls>("mock")

    private fun expectVerificationError(vararg messages: String, block: () -> Unit) {
        try {
            clearMocks(mock, openMock)
            block()
            fail("Block should throw verification failure")
        } catch (ex: AssertionError) {
            if (messages.any { !ex.message!!.contains(it) }) {
                fail("Bad message: " + ex.message)
            }
        }
    }

    @Test
    fun notMatchingArguments() {
        expectVerificationError(
            "Only one matching call to ",
            "but arguments are not matching",
            "MockCls.otherOp"
        ) {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify { mock.otherOp(1, 3) }
        }
    }

    @Test
    fun notMatchingArgumentsWithSameMethod() {
        expectVerificationError("No matching calls found.", "Calls to same method", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 4)

            verify { mock.otherOp(1, 3) }
        }
    }

    @Test
    fun wasNotCalledToSameMock() {
        expectVerificationError("was not called", "Calls to same mock", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify { mock.manyArgsOp(true, false) }
        }
    }

    @Test
    fun wasNotCalled() {
        expectVerificationError("was not called") {
            verify { mock.otherOp(1, 2) }
        }
    }

    @Test
    fun someMatchingCallsFoundButNotAll() {
        expectVerificationError("2 matching calls found, but needs at least 3 calls", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 2)

            verify(atLeast = 3) { mock.otherOp(1, 2) }
        }
    }

    @Test
    fun oneMatchingCallFoundButNeedMore() {
        expectVerificationError("One matching call found, but needs at least 3 calls", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)

            verify(atLeast = 3) { mock.otherOp(1, 2) }
        }
    }

    @Test
    fun callsAreNotInVerificationOrder() {
        expectVerificationError("calls are not in verification order", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifyOrder {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
    }

    @Test
    fun lessCallsHappenedThenDemanded() {
        expectVerificationError("less calls happened then demanded by order verification sequence", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 3)

            verifyOrder {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
    }

    @Test
    fun notFullSequence() {
        expectVerificationError(
            "number of calls happened not matching exact number of verification sequence",
            "MockCls.otherOp"
        ) {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 3)

            verifySequence {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
    }

    @Test
    fun callsNotMatchinVerificationSequence() {
        expectVerificationError("calls are not exactly matching verification sequence", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifySequence {
                mock.otherOp(1, 3)
                mock.otherOp(1, 2)
            }
        }
    }

    @Test
    fun someCallsWereNotMatched() {
        expectVerificationError("some calls were not matched", "MockCls.otherOp") {
            every { mock.otherOp(1, any()) } answers { 2 + firstArg<Int>() }

            mock.otherOp(1, 2)
            mock.otherOp(1, 3)

            verifyAll {
                mock.otherOp(1, 2)
            }
        }
    }

    @Test
    @SkipInstrumentedAndroidTest
    fun byteBuddyContraction() {
        expectVerificationError("MockCls(BB).op") {
            every { openMock.op(1, any()) } returns 3

            openMock.op(1, 2)
            openMock.op(1, 3)

            verifyAll {
                openMock.op(1, 2)
            }
        }
    }

    data class IntWrapper(val data: Int)

    class MockCls {
        fun otherOp(a: Int = 1, b: Int = 2): Int = a + b

        fun manyArgsOp(
            a: Boolean = true, b: Boolean = true,
            c: Byte = 1, d: Byte = 2,
            e: Short = 3, f: Short = 4,
            g: Char = 5.toChar(), h: Char = 6.toChar(),
            i: Int = 7, j: Int = 8,
            k: Long = 9, l: Long = 10,
            m: Float = 10.0f, n: Float = 11.0f,
            o: Double = 12.0, p: Double = 13.0,
            q: String = "14", r: String = "15",
            s: IntWrapper = IntWrapper(16), t: IntWrapper = IntWrapper(17)
        ): Double {

            return (if (a) 0 else -1) + (if (b) 0 else -2) + c + d + e + f + g.toByte() + h.toByte() +
                    i + j + k + l + m + n + o + p + q.toInt() + r.toInt() + s.data + t.data
        }
    }

    abstract class OpenMockCls {
        abstract fun op(a: Int = 1, b: Int = 2): Int
    }
}