package io.mockk.impl.recording

import io.mockk.Invocation
import io.mockk.Matcher
import io.mockk.impl.log.SafeToString
import kotlin.reflect.KClass

class CallRoundBuilder(val safeToString: SafeToString) {
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
            safeToString.exec { invocation.toString() }
        )

        signedCalls.add(signedCall)
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
                    safeToString.exec { "$self wasNot Called" }
                )
            )
        }
    }

    fun build() = CallRound(signedCalls.toList(), signedMatchers.toList())
}
