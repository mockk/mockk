package io.mockk.impl.recording

import io.mockk.every
import io.mockk.impl.log.SafeLog
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class ChainedCallDetectorTest {
    val safeLog = mockk<SafeLog>(relaxed = true)
    val detector = ChainedCallDetector(safeLog)
    val callRound1 = mockk<CallRound>(relaxed = true)
    val callRound2 = mockk<CallRound>(relaxed = true)
    val call1 = mockk<SignedCall>(relaxed = true)
    val call2 = mockk<SignedCall>(relaxed = true)
    val signedMatcher1 = mockk<SignedMatcher>(relaxed = true)
    val signedMatcher2 = mockk<SignedMatcher>(relaxed = true)

    @Test
    fun givenTwoCallRoundsWithOneCallNoArgsWhenDetectCallsHappenThenOneCallIsReturned() {

        every { callRound1.calls[0] } returns call1
        every { callRound2.calls[0] } returns call2

        every { call1.method.name } returns "abc"
        every { call2.method.name } returns "abc"

        every { call1.method.varArgsArg } returns -1
        every { call2.method.varArgsArg } returns -1

        detector.detect(listOf(callRound1, callRound2), 0)

        assertEquals("abc", detector.call.matcher.method.name)
    }

    @Test
    fun givenTwoCallsRoundsWithOneCallOneArgWhenDetectCallsHappenThenOneCallWithArgIsReturned() {
        every { callRound1.calls[0] } returns call1
        every { callRound2.calls[0] } returns call2

        every { call1.matchers } returns listOf(signedMatcher1)
        every { signedMatcher1.signature } returns 5

        every { call2.matchers } returns listOf(signedMatcher2)
        every { signedMatcher2.signature } returns 6

        every { call1.args } returns listOf(5)
        every { call2.args } returns listOf(6)

        every { call1.method.name } returns "abc"
        every { call2.method.name } returns "abc"

        every { call1.method.varArgsArg } returns -1
        every { call2.method.varArgsArg } returns -1

        detector.detect(listOf(callRound1, callRound2), 0)

        assertEquals("abc", detector.call.matcher.method.name)
        assertEquals(listOf(signedMatcher2.matcher), detector.call.matcher.args)
    }
}