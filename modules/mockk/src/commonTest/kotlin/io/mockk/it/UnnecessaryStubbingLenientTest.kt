package io.mockk.it

import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.justRun
import io.mockk.lenient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("UNUSED_PARAMETER")
class UnnecessaryStubbingLenientTest {
    class MockCls {
        fun op(a: Int) = a + 1

        fun other(a: Int) = a + 2

        fun unitOp(a: Int) {}

        suspend fun coOp(a: Int) = a + 1
    }

    @Test
    fun lenientStubThatIsNeverUsedIsNotReported() {
        val mock = mockk<MockCls>()

        lenient {
            every { mock.op(1) } returns 1
        }

        checkUnnecessaryStub(mock)
    }

    @Test
    fun regularStubThatIsNeverUsedIsStillReported() {
        val mock = mockk<MockCls>()

        every { mock.op(1) } returns 1

        val error =
            assertFailsWith<AssertionError> {
                checkUnnecessaryStub(mock)
            }
        assertTrue(error.message!!.contains("op(eq(1))"))
    }

    @Test
    fun lenientBlockOnlyExemptsStubsDeclaredInsideIt() {
        val mock = mockk<MockCls>()

        every { mock.op(98) } returns 1
        lenient {
            every { mock.op(99) } returns 2
        }

        val error =
            assertFailsWith<AssertionError> {
                checkUnnecessaryStub(mock)
            }
        assertTrue(error.message!!.contains("op(eq(98))"))
        assertTrue(!error.message!!.contains("op(eq(99))"))
    }

    @Test
    fun stubDeclaredAfterTheLenientBlockIsStillReported() {
        val mock = mockk<MockCls>()

        lenient {
            every { mock.op(99) } returns 2
        }
        every { mock.op(98) } returns 1

        val error =
            assertFailsWith<AssertionError> {
                checkUnnecessaryStub(mock)
            }
        assertTrue(error.message!!.contains("op(eq(98))"))
        assertTrue(!error.message!!.contains("op(eq(99))"))
    }

    @Test
    fun eachLenientBlockAffectsOnlyItsOwnStubs() {
        val mock = mockk<MockCls>()

        lenient {
            every { mock.op(1) } returns 1
        }
        every { mock.op(2) } returns 2
        lenient {
            every { mock.op(3) } returns 3
        }

        val error =
            assertFailsWith<AssertionError> {
                checkUnnecessaryStub(mock)
            }
        assertTrue(error.message!!.contains("op(eq(2))"))
        assertTrue(!error.message!!.contains("op(eq(1))"))
        assertTrue(!error.message!!.contains("op(eq(3))"))
    }

    @Test
    fun lenientStubIsUsedWhenTheMockIsCalled() {
        val mock = mockk<MockCls>()

        lenient {
            every { mock.op(1) } returns 1
        }

        assertEquals(1, mock.op(1))

        checkUnnecessaryStub(mock)
    }

    @Test
    fun lenientStubNotMatchedByTheActualCallIsStillNotReported() {
        val mock = mockk<MockCls>()

        every { mock.op(2) } returns 3
        lenient {
            every { mock.op(1) } returns 1
        }

        assertEquals(3, mock.op(2))

        checkUnnecessaryStub(mock)
    }

    @Test
    fun usedRegularStubTogetherWithUnusedLenientStubIsAccepted() {
        val mock = mockk<MockCls>()

        every { mock.op(1) } returns 1
        lenient {
            every { mock.op(2) } returns 2
            every { mock.other(3) } returns 3
        }

        assertEquals(1, mock.op(1))

        checkUnnecessaryStub(mock)
    }

    @Test
    fun chainedAnswersOnALenientStubAreExempt() {
        val mock = mockk<MockCls>()

        lenient {
            every { mock.op(1) } returns 1 andThen 2
        }

        checkUnnecessaryStub(mock)
    }

    @Test
    fun justRunInsideALenientBlockIsExempt() {
        val mock = mockk<MockCls>()

        lenient {
            justRun { mock.unitOp(1) }
        }

        checkUnnecessaryStub(mock)
    }

    @Test
    fun justRunOutsideALenientBlockIsStillReported() {
        val mock = mockk<MockCls>()

        lenient {
            every { mock.op(1) } returns 1
        }
        justRun { mock.unitOp(2) }

        val error =
            assertFailsWith<AssertionError> {
                checkUnnecessaryStub(mock)
            }
        assertTrue(error.message!!.contains("unitOp(eq(2))"))
        assertTrue(!error.message!!.contains("op(eq(1))"))
    }

    @Test
    fun coEveryInsideALenientBlockIsExempt() =
        runTest {
            val mock = mockk<MockCls>()

            lenient {
                coEvery { mock.coOp(1) } returns 1
            }

            checkUnnecessaryStub(mock)
        }

    @Test
    fun coJustRunInsideALenientBlockIsExempt() =
        runTest {
            val mock = mockk<MockCls>()

            lenient {
                coJustRun { mock.coOp(1) }
            }

            checkUnnecessaryStub(mock)
        }

    @Test
    fun checkUnnecessaryStubOverAllMocksSkipsLenientStubsAndReportsRegularOnes() {
        clearAllMocks() // ensure that previous tests haven't created unnecessary stubs

        val mock1 = mockk<MockCls>()
        val mock2 = mockk<MockCls>()

        every { mock1.op(5) } returns 1
        mock1.op(5)

        lenient {
            every { mock2.op(42) } returns 1
        }
        every { mock2.op(43) } returns 2

        val error =
            assertFailsWith<AssertionError> {
                checkUnnecessaryStub()
            }
        assertTrue(error.message!!.contains("op(eq(43))"))
        assertTrue(!error.message!!.contains("op(eq(42))"))
    }
}
