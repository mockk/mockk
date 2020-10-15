package io.mockk.impl.recording

import io.mockk.Matcher

class SignatureMatchersList {
    private var matchers = mutableListOf<SignatureWithMatcher>()

    fun add(signature: List<Any>, matcher: Matcher<*>) {
        matchers.add(SignatureWithMatcher(signature, matcher))
    }

    fun remove(signature: List<Any?>): Matcher<*>? {
        val index = matchers.indexOfFirst { it.signature == signature }
        return if (index > -1) matchers.removeAt(index).matcher else null
    }

    fun isNotEmpty() = matchers.isNotEmpty()

    override fun toString(): String {
        return matchers.map { it.matcher }.toString()
    }

    private data class SignatureWithMatcher(val signature: List<Any>, val matcher: Matcher<*>)
}