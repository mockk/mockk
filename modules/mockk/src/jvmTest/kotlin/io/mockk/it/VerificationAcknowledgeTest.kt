package io.mockk.it

import io.mockk.Called
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyCount
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class VerificationAcknowledgeTest {
    class MockCls {
        fun op(a: Int) = a + 1
    }

    val mock = mockk<MockCls>()

    private fun doCalls1() {
        every { mock.op(5) } returns 1
        every { mock.op(6) } returns 2
        every { mock.op(7) } returns 3

        assertEquals(1, mock.op(5))
        assertEquals(2, mock.op(6))
        assertEquals(3, mock.op(7))
    }


    private fun doCalls2() {
        every { mock.op(0) } throws RuntimeException("test")
        every { mock.op(1) } returnsMany listOf(1, 2, 3)

        assertFailsWith(RuntimeException::class) {
            mock.op(0)
        }

        assertEquals(1, mock.op(1))
        assertEquals(2, mock.op(1))
        assertEquals(3, mock.op(1))
        assertEquals(3, mock.op(1))
    }

    @Test
    fun `checkUnnecessaryStub with single call to each stub`() {
        doCalls1()
        checkUnnecessaryStub(mock)
    }

    @Test
    fun `checkUnnecessaryStub with multiple calls to some stub`() {
        doCalls2()
        checkUnnecessaryStub(mock)
    }

    @Test
    fun `checkUnnecessaryStub with some used and some unused stubs`() {
        every { mock.op(98) } returns 1
        every { mock.op(99) } returns 2
        doCalls1()
        assertFailsWith<AssertionError> {
            checkUnnecessaryStub(mock)
        }
            .also {
                assertThat(
                    it.message, equalTo(
                        "Unnecessary stubbings detected.\n" +
                                "Following stubbings are not used, either because there are unnecessary or because tested code doesn't call them :\n" +
                                "\n" +
                                "1) ${mock}.op(eq(98)))\n" +
                                "2) ${mock}.op(eq(99)))"
                    )
                )
            }
    }

    @Test
    fun `checkUnnecessaryStubAll with single call to each stub`() {
        clearAllMocks() // ensure that previous tests haven't created unnecessary stubs
        doCalls1()
        val mock2 = mockk<MockCls>()
        every { mock2.op(98) } returns 1
        mock2.op(98)
        checkUnnecessaryStub()
    }

    @Test
    fun `checkUnnecessaryStubAll with some used and some unused stubs`() {
        clearAllMocks() // ensure that previous tests haven't created unnecessary stubs
        doCalls1()
        val mock2 = mockk<MockCls> { every { op(42) } returns 1 }
        assertFailsWith<AssertionError> {
            checkUnnecessaryStub()
        }
            .also {
                assertThat(
                    it.message, equalTo(
                        "Unnecessary stubbings detected.\n" +
                                "Following stubbings are not used, either because there are unnecessary or because tested code doesn't call them :\n" +
                                "\n" +
                                "1) ${mock2}.op(eq(42)))"
                    )
                )
            }
    }

    @Test
    fun checkVerify() {
        doCalls1()

        verify {
            mock.op(6)
            mock.op(5)
        }

        excludeRecords {
            mock.op(7)
        }

        confirmVerified(mock)
    }

    @Test
    fun checkVerifyPreset() {
        excludeRecords(current = false) {
            mock.op(7)
        }

        doCalls1()

        verify {
            mock.op(6)
            mock.op(5)
        }

        confirmVerified(mock)
    }

    @Test
    fun checkVerifyNoCurrentExclusion() {
        doCalls1()

        verify {
            mock.op(6)
            mock.op(5)
        }

        excludeRecords(current = false) {
            mock.op(7)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun checkVerifyInverse1() {
        doCalls1()

        verify(inverse = true) {
            mock.op(6)
            mock.op(8)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun checkVerifyInverse2() {
        doCalls1()

        verify(inverse = true) {
            mock.op(4)
            mock.op(8)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun checkVerifyOrder1() {
        doCalls1()

        verifyOrder {
            mock.op(5)
            mock.op(7)
        }

        excludeRecords { mock.op(6) }
        confirmVerified(mock)
    }

    @Test
    fun checkVerifyOrder2() {
        doCalls1()

        verifyOrder {
            mock.op(5)
            mock.op(6)
        }

        excludeRecords { mock.op(7) }
        confirmVerified(mock)
    }

    @Test
    fun checkVerifyOrder3() {
        doCalls1()

        verifyOrder {
            mock.op(6)
            mock.op(7)
        }

        excludeRecords { mock.op(5) }
        confirmVerified(mock)
    }

    @Test
    fun checkVerifyOrderInverse1() {
        doCalls1()

        verifyOrder(inverse = true) {
            mock.op(7)
            mock.op(5)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun checkVerifyOrderInverse2() {
        doCalls1()

        verifyOrder(inverse = true) {
            mock.op(5)
            mock.op(4)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun checkVerifyOrderInverse3() {
        doCalls1()

        verifyOrder(inverse = true) {
            mock.op(4)
            mock.op(8)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun verifySequence() {
        doCalls1()

        verifySequence {
            mock.op(5)
            mock.op(6)
            mock.op(7)
        }

        confirmVerified(mock)
    }

    @Test
    fun verifySequenceInverse1() {
        doCalls1()

        verifySequence(inverse = true) {
            mock.op(6)
            mock.op(7)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun verifySequenceInverse2() {
        doCalls1()

        verifySequence(inverse = true) {
            mock.op(7)
            mock.op(6)
            mock.op(5)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun verifySequenceInverse3() {
        doCalls1()

        verifySequence(inverse = true) {
            mock.op(6)
            mock.op(5)
            mock.op(7)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun verifyCount() {
        doCalls1()

        verifyCount {
            1 * { mock.op(6) }
            1 * { mock.op(5) }
            1 * { mock.op(7) }
        }

        confirmVerified(mock)
    }

    @Test
    fun verifyCount2() {
        doCalls1()

        verifyCount {
            1 * { mock.op(6) }
            1 * { mock.op(5) }
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun atLeast() {
        doCalls2()

        verify(atLeast = 4) {
            mock.op(1)
        }

        excludeRecords { mock.op(0) }
        confirmVerified(mock)
    }

    @Test
    fun atLeastInverse() {
        doCalls2()

        verify(atLeast = 5, inverse = true) {
            mock.op(1)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun exactly() {
        doCalls2()

        verify(exactly = 4) {
            mock.op(1)
        }

        excludeRecords { mock.op(0) }
        confirmVerified(mock)
    }

    @Test
    fun exactlyInverse() {
        doCalls2()

        verify(exactly = 3, inverse = true) {
            mock.op(1)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun atMost() {
        doCalls2()

        verify(atMost = 4) {
            mock.op(1)
        }

        excludeRecords { mock.op(0) }
        confirmVerified(mock)
    }

    @Test
    fun atMostInverse() {
        doCalls2()

        verify(atMost = 3, inverse = true) {
            mock.op(1)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun exactlyZero() {
        doCalls2()

        verify(exactly = 0) {
            mock.op(3)
        }

        assertFails {
            confirmVerified(mock)
        }
    }


    @Test
    fun exactlyOnce() {
        doCalls2()

        verify(exactly = 1) {
            mock.op(0)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun exactlyTwiceInverse() {
        doCalls2()

        verify(exactly = 2, inverse = true) {
            mock.op(0)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun exactlyZeroInverse() {
        doCalls2()

        verify(exactly = 0, inverse = true) {
            mock.op(0)
        }

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun wasNotCalled() {
        val secondMock = mockk<MockCls>()
        val thirdMock = mockk<MockCls>()

        verify {
            listOf(secondMock, thirdMock) wasNot Called
        }

        confirmVerified(secondMock, thirdMock)
    }

    @Test
    fun simple() {
        doCalls2()

        verify { mock.op(0) }

        excludeRecords { mock.op(1) }
        confirmVerified(mock)
    }

    @Test
    fun order() {
        doCalls2()

        verifyOrder {
            mock.op(1)
            mock.op(1)
            mock.op(1)
            mock.op(1)
        }

        excludeRecords { mock.op(0) }
        confirmVerified(mock)
    }

    @Test
    fun sequence() {
        doCalls2()

        verifySequence {
            mock.op(0)
            mock.op(1)
            mock.op(1)
            mock.op(1)
            mock.op(1)
        }

        confirmVerified(mock)
    }

    @Test
    fun clearExclusions() {

        every { mock.op(1) } returns 1
        every { mock.op(2) } returns 2
        every { mock.op(3) } returns 3
        every { mock.op(4) } returns 4

        assertEquals(1, mock.op(1))
        assertEquals(2, mock.op(2))
        assertEquals(3, mock.op(3))
        assertEquals(4, mock.op(4))

        excludeRecords { mock.op(more(3, andEquals = true)) }

        verifySequence {
            mock.op(1)
            mock.op(2)
        }

        assertEquals(3, mock.op(3))
        assertEquals(4, mock.op(4))

        verifySequence {
            mock.op(1)
            mock.op(2)
        }

        clearMocks(
            mock,
            answers = false,
            recordedCalls = false,
            childMocks = false,
            verificationMarks = false,
            exclusionRules = true
        )

        assertEquals(3, mock.op(3))
        assertEquals(4, mock.op(4))

        verifySequence {
            mock.op(1)
            mock.op(2)
            mock.op(3)
            mock.op(4)
        }

        confirmVerified(mock)
    }

    @Test
    fun clearMarks() {

        every { mock.op(1) } returns 1
        every { mock.op(2) } returns 2
        every { mock.op(3) } returns 3
        every { mock.op(4) } returns 4

        assertEquals(1, mock.op(1))
        assertEquals(2, mock.op(2))
        assertEquals(3, mock.op(3))
        assertEquals(4, mock.op(4))

        verify {
            mock.op(1)
            mock.op(2)
            mock.op(3)
        }

        assertFails {
            confirmVerified(mock)
        }

        verify {
            mock.op(4)
        }

        confirmVerified(mock)

        clearMocks(
            mock,
            answers = false,
            recordedCalls = false,
            childMocks = false,
            verificationMarks = true,
            exclusionRules = false
        )

        assertFails {
            confirmVerified(mock)
        }
    }

    @Test
    fun confirmVerifiedAll() {
        clearAllMocks()
        doCalls1()

        verify {
            mock.op(5)
            mock.op(6)
            mock.op(7)
        }

        confirmVerified()
    }

    @Test
    fun confirmVerifiedAllInverse() {
        clearAllMocks()
        doCalls1()

        verify {
            mock.op(5)
            mock.op(6)
        }

        assertFails {
            confirmVerified()
        }
    }

    @Test
    fun confirmVerifiedAllExclude() {
        clearAllMocks()
        excludeRecords(current = false) {
            mock.op(7)
        }

        doCalls1()

        verify {
            mock.op(6)
            mock.op(5)
        }

        confirmVerified()
    }
}

