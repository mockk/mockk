package io.mockk.impl.recording

import io.mockk.Matcher

class SignatureMatchersMap {
    private var matchers = mutableMapOf<List<Any?>, List<Matcher<*>>>()

    fun add(signature: List<Any?>, matcher: Matcher<*>) {
        val signatureMatchers = matchers[signature] ?: emptyList()
        matchers[signature] = signatureMatchers.plus(matcher)
    }

    fun remove(signature: List<Any?>): Matcher<*>? {
        val signatureMatchers = (matchers[signature] ?: emptyList()).toMutableList()
        val removedMatcher = if (signatureMatchers.size > 0) signatureMatchers.removeAt(0) else null

        if (signatureMatchers.isEmpty()) {
            matchers.remove(signature) }
        else {
            matchers[signature] = signatureMatchers.toList()
        }

        return removedMatcher
    }

    fun isNotEmpty() = matchers.isNotEmpty()

    override fun toString(): String {
        return matchers.flatMap { it.value }.toString()
    }
}