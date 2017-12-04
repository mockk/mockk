package io.mockk.impl

import io.mockk.Invocation
import io.mockk.Matcher
import kotlin.reflect.KClass

class CallRoundBuilder {
    val signedCalls = mutableListOf<SignedCall>()

    val matchers = mutableListOf<Matcher<*>>()
    val signatures = mutableListOf<Any>()

    fun addMatcher(matcher: Matcher<*>, ref: Any) {
        matchers.add(matcher)
        signatures.add(ref)
    }

    fun addSignedCall(retType: KClass<*>, invocation: Invocation) {
        signedCalls.add(SignedCall(retType, invocation, matchers.toList(), signatures.toList()))

        matchers.clear()
        signatures.clear()
    }

    fun build() = CallRound(signedCalls.toList())
}