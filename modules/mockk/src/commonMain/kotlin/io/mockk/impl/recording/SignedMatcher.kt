package io.mockk.impl.recording

import io.mockk.Matcher

data class SignedMatcher(
    val matcher: Matcher<*>,
    val signature: Any
)