package io.mockk.impl.recording

import io.mockk.EquivalentMatcher
import io.mockk.MatchedCall
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeLog

class RealChildMocker(val stubRepo: StubRepository,
                      val safeLog: SafeLog) {

    val log = safeLog(Logger<RealChildMocker>())

    private var newSelf: Any? = null
    private val resultCalls = mutableListOf<MatchedCall>()

    fun mock(calls: List<MatchedCall>): MutableList<MatchedCall> {
        newSelf = null
        for ((idx, call) in calls.withIndex()) {
            mockCall(calls, idx, call)
        }
        return resultCalls
    }

    private fun mockCall(calls: List<MatchedCall>, idx: Int, call: MatchedCall) {
        val isLastCall = idx == calls.size - 1

        val invocation = call.invocation

        if (!call.chained) {
            newSelf = invocation.self
        }

        val newInvocation = call.invocation.copy(self = newSelf!!)
        val newMatcher = call.matcher.copy(self = newSelf!!)
        val newCall = call.copy(invocation = newInvocation, matcher = newMatcher)

        resultCalls.add(newCall)

        if (isLastCall || !calls[idx + 1].chained) return

        val args = newCall.matcher.args.map {
            when (it) {
                is EquivalentMatcher -> it.equivalent()
                else -> it
            }
        }

        val invocationMatcher = newCall.matcher.copy(args = args)
        val equivalentCall = newCall.copy(matcher = invocationMatcher)

        log.trace { "Child search key: $invocationMatcher" }

        newSelf = stubRepo.stubFor(newSelf!!)
                .childMockK(equivalentCall.matcher, equivalentCall.retType)
    }
}