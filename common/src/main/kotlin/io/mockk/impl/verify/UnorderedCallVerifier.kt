package io.mockk.impl.verify

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MatchedCall
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationResult
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.formatCalls

open class UnorderedCallVerifier(val stubRepo: StubRepository) : CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(calls: List<MatchedCall>, min: Int, max: Int): VerificationResult {
        for ((i, call) in calls.withIndex()) {
            val result = matchCall(call, min, max, "call ${i + 1} of ${calls.size}: ${call.invocation}.")

            if (!result.matches) {
                return result
            }
        }
        return VerificationResult(true)
    }

    private fun matchCall(templateCall: MatchedCall, min: Int, max: Int, callIdxMsg: String): VerificationResult {
        val stub = stubRepo.stubFor(templateCall.invocation.self)
        val allCallsForMock = stub.allRecordedCalls()
        val allCallsForMockMethod = allCallsForMock.filter {
            templateCall.matcher.method == it.method
        }
        val result = when (allCallsForMockMethod.size) {
            0 -> {
                if (min == 0 && max == 0) {
                    VerificationResult(true)
                } else if (allCallsForMock.isEmpty()) {
                    VerificationResult(false, "$callIdxMsg ${stub.toStr()}/${templateCall.matcher.method.toStr()} was not called")
                } else {
                    VerificationResult(false, "$callIdxMsg ${stub.toStr()}/${templateCall.matcher.method.toStr()} was not called.\n" +
                            "Calls to same mock:\n" + formatCalls(allCallsForMock))
                }
            }
            1 -> {
                val onlyCall = allCallsForMockMethod.get(0)
                if (templateCall.matcher.match(onlyCall)) {
                    if (1 in min..max) {
                        VerificationResult(true)
                    } else {
                        VerificationResult(false, "$callIdxMsg One matching call found, but needs at least $min${atMostMsg(max)} calls")
                    }
                } else {
                    VerificationResult(false, "$callIdxMsg Only one matching call to ${stub.toStr()}/${templateCall.matcher.method.toStr()} happened, but arguments are not matching:\n" +
                            describeArgumentDifference(templateCall.matcher, onlyCall))
                }
            }
            else -> {
                val n = allCallsForMockMethod.filter { templateCall.matcher.match(it) }.count()
                if (n in min..max) {
                    VerificationResult(true)
                } else {
                    if (n == 0) {
                        VerificationResult(false,
                                "$callIdxMsg No matching calls found.\n" +
                                        "Calls to same method:\n" + formatCalls(allCallsForMockMethod))
                    } else {
                        VerificationResult(false,
                                "$callIdxMsg $n matching calls found, " +
                                        "but needs at least $min${atMostMsg(max)} calls")
                    }
                }
            }
        }

        captureBlocks.add({
            for (call in allCallsForMockMethod) {
                templateCall.matcher.captureAnswer(call)
            }
        })

        return result
    }

    override fun captureArguments() {
        captureBlocks.forEach { it() }
    }

    private fun atMostMsg(max: Int) = if (max == Int.MAX_VALUE) "" else " and at most $max"

    private fun describeArgumentDifference(matcher: InvocationMatcher,
                                           invocation: Invocation): String {
        val str = StringBuilder()
        for ((i, arg) in invocation.args.withIndex()) {
            val argMatcher = matcher.args[i]
            val matches = argMatcher.match(arg)
            str.append("[$i]: argument: $arg, matcher: $argMatcher, result: ${if (matches) "+" else "-"}\n")
        }
        return str.toString()
    }
}