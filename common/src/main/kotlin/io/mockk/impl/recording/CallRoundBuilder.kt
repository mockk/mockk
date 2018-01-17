package io.mockk.impl.recording

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.impl.log.SafeLog
import kotlin.reflect.KClass

class CallRoundBuilder(val safeLog: SafeLog) {
    val signedMatchers = mutableListOf<SignedMatcher>()
    val signedCalls = mutableListOf<SignedCall>()

    fun addMatcher(matcher: Matcher<*>, sigValue: Any) {
        signedMatchers.add(SignedMatcher(matcher, sigValue))
    }

    fun addSignedCall(
        retValue: Any?,
        tempMock: Boolean,
        retType: KClass<*>,
        invocation: Invocation
    ) {
        val signedCall = SignedCall(
            retValue,
            tempMock,
            retType,
            invocation.self,
            invocation.method,
            invocation.args,
            safeLog.exec { invocation.toString() },
            signedMatchers.toList()
        )

        signedCalls.add(signedCall)
        signedMatchers.clear()
    }

    fun addWasNotCalled(list: List<Any>) {
        for (self in list) {
            signedCalls.add(
                SignedCall(
                    Unit,
                    false,
                    Unit::class,
                    self,
                    WasNotCalled.method,
                    listOf(),
                    safeLog.exec { "${self} wasNot Called" },
                    listOf()
                )
            )
        }
    }

    fun build() = CallRound(signedCalls.toList())
}