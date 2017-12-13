package io.mockk.impl.recording

import io.mockk.EquivalentMatcher
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeLog

class PermanentMocker(val stubRepo: StubRepository,
                      val safeLog: SafeLog) {

    val log = safeLog(Logger<PermanentMocker>())

    val permanentMocks = InternalPlatform.weakMap<Any, Any>()
    val callRef = InternalPlatform.weakMap<Any, RecordedCall>()

    fun mock(calls: List<RecordedCall>): List<RecordedCall> {
        val result = mutableListOf<RecordedCall>()
        for (call in calls) {
            val permanentCall = mockCall(call)
            result.add(permanentCall)
        }
        return result
    }

    private fun mockCall(call: RecordedCall): RecordedCall {
        val newCall = makePermanent(call)

        val retValue = call.retValue
        if (call.isRetValueMock && retValue != null) {
            val equivalentCall = makeEquivalent(newCall)

            log.trace { "Child search key: ${equivalentCall.matcher}" }

            val childMock = stubRepo.stubFor(newCall.matcher.self)
                    .childMockK(equivalentCall.matcher, equivalentCall.retType)

            permanentMocks[retValue] = childMock
            callRef[retValue] = newCall
        }

        return newCall
    }

    private fun makeEquivalent(newCall: RecordedCall): RecordedCall {
        val equivalentArgs = newCall.matcher.args.map {
            when (it) {
                is EquivalentMatcher -> it.equivalent()
                else -> it
            }
        }

        val equivalentIM = newCall.matcher.copy(args = equivalentArgs)
        return newCall.copy(matcher = equivalentIM)
    }

    private fun makePermanent(call: RecordedCall): RecordedCall {
        val selfChain = callRef[call.matcher.self]
        val newSelf = permanentMocks[call.matcher.self] ?: call.matcher.self
        val newArgs = call.matcher.args.map { it.substitute(permanentMocks) }
        val newMatcher = call.matcher.copy(self = newSelf, args = newArgs)
        return call.copy(matcher = newMatcher, selfChain = selfChain)
    }
}