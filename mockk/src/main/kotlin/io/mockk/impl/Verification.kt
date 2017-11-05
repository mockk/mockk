package io.mockk.impl

import io.mockk.*
import io.mockk.external.logger
import java.lang.AssertionError

internal class VerifierImpl(gw: MockKGatewayImpl) : CommonRecorder(gw), Verifier {
    private val log = logger<VerifierImpl>()

    override fun <T> verify(ordering: Ordering, inverse: Boolean, atLeast: Int, atMost: Int, exactly: Int,
                            mockBlock: (MockKVerificationScope.() -> T)?,
                            coMockBlock: (suspend MockKVerificationScope.() -> T)?) {
        if (ordering != Ordering.UNORDERED) {
            if (atLeast != 1 || atMost != Int.MAX_VALUE || exactly != -1) {
                throw MockKException("atLeast, atMost, exactly is only allowed in unordered verify block")
            }
        }

        val gw = MockKGateway.LOCATOR()
        val callRecorder = gw.callRecorder
        callRecorder.startVerification()

        val lambda = slot<Function<*>>()
        val scope = MockKVerificationScope(gw, lambda)

        try {
            record(scope, mockBlock, coMockBlock)
        } catch (ex: NoClassDefFoundError) {
            callRecorder.cancel()
            throw prettifyCoroutinesException(ex)
        } catch (ex: Throwable) {
            callRecorder.cancel()
            throw ex
        }

        try {
            val min = if (exactly != -1) exactly else atLeast
            val max = if (exactly != -1) exactly else atMost

            val outcome = gw.verifier(ordering).verify(callRecorder.calls, min, max)

            log.trace { "Done verification. Outcome: $outcome" }

            failIfNotPassed(outcome, inverse)
        } catch (ex: Throwable) {
            callRecorder.cancel()
            throw ex
        } finally {
            callRecorder.doneVerification()
        }
    }

    private fun failIfNotPassed(outcome: VerificationResult, inverse: Boolean) {
        val matcherStr = if (outcome.matcher != null) ", matcher: ${outcome.matcher}" else ""

        if (inverse) {
            if (outcome.matches) {
                throw AssertionError("Inverse verification failed$matcherStr")
            }
        } else {
            if (!outcome.matches) {
                throw AssertionError("Verification failed$matcherStr")
            }
        }
    }


}

internal class UnorderedCallVerifierImpl(private val gw: MockKGatewayImpl) : CallVerifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        return calls
                .firstOrNull { !it.invocation.self().___matchesAnyRecordedCalls(it.matcher, min, max) }
                ?.matcher
                ?.let { VerificationResult(false, it) }
                ?: VerificationResult(true)
    }
}

private fun List<Call>.allCalls() =
        this.map { Ref(it.invocation.self) }
                .distinct()
                .map { it.value as MockKInstance }
                .flatMap { it.___allRecordedCalls() }
                .sortedBy { it.timestamp }

internal class OrderedCallVerifierImpl(private val gw: MockKGatewayImpl) : CallVerifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        val allCalls = calls.allCalls()

        if (calls.size > allCalls.size) {
            return VerificationResult(false)
        }

        // LCS algorithm
        var prev = Array(calls.size, { 0 })
        var curr = Array(calls.size, { 0 })
        for (call in allCalls) {
            for ((matcherIdx, matcher) in calls.map { it.matcher }.withIndex()) {
                curr[matcherIdx] = if (matcher.match(call)) {
                    if (matcherIdx == 0) 1 else prev[matcherIdx - 1] + 1
                } else {
                    maxOf(prev[matcherIdx], if (matcherIdx == 0) 0 else curr[matcherIdx - 1])
                }
            }
            val swap = curr
            curr = prev
            prev = swap
        }

        // match only if all matchers present
        return VerificationResult(prev.last() == calls.size)
    }
}

internal class SequenceCallVerifierImpl(private val gw: MockKGatewayImpl) : CallVerifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        val allCalls = calls.allCalls()

        if (allCalls.size != calls.size) {
            return VerificationResult(false)
        }

        for ((i, call) in allCalls.withIndex()) {
            if (!calls[i].matcher.match(call)) {
                return VerificationResult(false)
            }
        }

        return VerificationResult(true)
    }
}

private fun Invocation.self() = self as MockKInstance
