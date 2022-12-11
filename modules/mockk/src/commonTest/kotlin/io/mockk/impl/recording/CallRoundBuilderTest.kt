package io.mockk.impl.recording

import io.mockk.*
import io.mockk.impl.log.SafeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CallRoundBuilderTest {
    val safeToString = mockk<SafeToString>(relaxed = true)
    val callRoundBuilder = CallRoundBuilder(safeToString)
    val matcher = mockk<Matcher<*>>(relaxed = true)
    val invocation = mockk<Invocation>(relaxed = true)
    val wasNotCalled = mockk<Any>(relaxed = true)

    @Test
    internal fun givenSignedCallWithOneMatcherWhenItsBuiltThenListOfCallsReturned() {
        every { safeToString.exec<Any>(captureLambda()) } answers { lambda<() -> Any>().invoke() }
        every { invocation.toString() } returns "Abc"
        callRoundBuilder.addMatcher(matcher, 5)
        callRoundBuilder.addSignedCall(5, false, Int::class, invocation)

        assertEquals(1, callRoundBuilder.signedCalls.size)
        assertEquals(5, callRoundBuilder.signedCalls[0].retValue)
        assertEquals(Int::class, callRoundBuilder.signedCalls[0].retType)
    }

    @Test
    internal fun givenWasNotCalledMockWhenItsAddedThenItIsReturnedInListOfCalls() {
        every { safeToString.exec<Any>(captureLambda()) } answers { lambda<() -> Any>().invoke() }
        every { invocation.toString() } returns "Abc"
        callRoundBuilder.addWasNotCalled(listOf(wasNotCalled))

        assertEquals(1, callRoundBuilder.signedCalls.size)
        assertSame(wasNotCalled, callRoundBuilder.signedCalls[0].self)
    }
}