package io.mockk.impl.recording

data class CallRound(
    val calls: List<SignedCall>,
    val matchers: List<SignedMatcher>
)