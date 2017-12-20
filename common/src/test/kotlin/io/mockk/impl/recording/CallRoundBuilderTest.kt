package io.mockk.impl.recording

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.impl.every
import io.mockk.impl.log.SafeLog
import io.mockk.impl.mockk
import io.mockk.invoke
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CallRoundBuilderTest {
    val safeLog = mockk<SafeLog>()
    val callRoundBuilder = CallRoundBuilder(safeLog)
    val matcher = mockk<Matcher<*>>()
    val invocation = mockk<Invocation>()
    val wasNotCalled = mockk<Any>()

    @Test
    internal fun givenSignedCallWithOneMatcherWhenItsBuiltThenListOfCallsReturned() {
        every { safeLog.exec<Any>(captureLambda()) } answers { lambda<() -> Any>().invoke() }
        every { invocation.toString() } returns "Abc"
        callRoundBuilder.addMatcher(matcher, 5)
        callRoundBuilder.addSignedCall(5, false, Int::class, invocation)

        assertEquals(1, callRoundBuilder.signedCalls.size)
        assertEquals(5, callRoundBuilder.signedCalls[0].retValue)
        assertEquals(Int::class, callRoundBuilder.signedCalls[0].retType)
    }

    @Test
    internal fun givenWasNotCalledMockWhenItsAddedThenItIsReturnedInListOfCalls() {
        every { safeLog.exec<Any>(captureLambda()) } answers { lambda<() -> Any>().invoke() }
        every { invocation.toString() } returns "Abc"
        callRoundBuilder.addWasNotCalled(listOf(wasNotCalled))

        assertEquals(1, callRoundBuilder.signedCalls.size)
        assertSame(wasNotCalled, callRoundBuilder.signedCalls[0].self)
    }
}