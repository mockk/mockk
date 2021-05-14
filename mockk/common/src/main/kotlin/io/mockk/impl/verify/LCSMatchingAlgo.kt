package io.mockk.impl.verify

import io.mockk.Invocation
import io.mockk.RecordedCall

class LCSMatchingAlgo(
    val allCalls: List<Invocation>,
    private val verificationSequence: List<RecordedCall>,
    private val captureBlocks: MutableList<() -> Unit>? = null
) {
    private val nEdits = Array(allCalls.size) { Array(verificationSequence.size) { 0 } }
    private val path = Array(allCalls.size) { Array(verificationSequence.size) { '?' } }

    val verifiedMatchers = mutableListOf<RecordedCall>()
    val verifiedCalls = mutableListOf<Invocation>()

    fun lcs(): Boolean {

        fun maxOf(a: Pair<Int, Char>, b: Pair<Int, Char>) =
            if (a.first > b.first) a else b

        for ((callIdx, call) in allCalls.withIndex()) {
            for ((matcherIdx, matcher) in verificationSequence.map { it.matcher }.withIndex()) {

                val result = if (matcher.match(call)) {
                    if (matcherIdx == 0 || callIdx == 0)
                        Pair(1, '=')
                    else
                        Pair(nEdits[callIdx - 1][matcherIdx - 1] + 1, '=')
                } else {
                    maxOf(
                        if (callIdx == 0)
                            Pair(0, '^')
                        else
                            Pair(nEdits[callIdx - 1][matcherIdx], '^'),

                        if (matcherIdx == 0)
                            Pair(0, '<')
                        else
                            Pair(nEdits[callIdx][matcherIdx - 1], '<')
                    )
                }

                nEdits[callIdx][matcherIdx] = result.first
                path[callIdx][matcherIdx] = result.second
            }
        }

        backTrackCalls(allCalls.size - 1, verificationSequence.size - 1)

        // match only if all matchers present
        return nEdits.getOrNull(allCalls.size - 1)?.getOrNull(verificationSequence.size -1) == verificationSequence.size
    }

    private tailrec fun backTrackCalls(callIdx: Int, matcherIdx: Int) {
        if (callIdx < 0 || matcherIdx < 0) return

        when (path[callIdx][matcherIdx]) {
            '=' -> {
                val matcher = verificationSequence[matcherIdx].matcher
                val invocation = allCalls[callIdx]
                captureBlocks?.add { matcher.captureAnswer(invocation) }
                verifiedCalls.add(invocation)
                verifiedMatchers.add(verificationSequence[matcherIdx])
                backTrackCalls(callIdx - 1, matcherIdx - 1)
            }
            '^' -> {
                backTrackCalls(callIdx - 1, matcherIdx)
            }
            '<' -> {
                backTrackCalls(callIdx, matcherIdx - 1)
            }
        }
    }
}
