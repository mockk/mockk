package io.mockk.common

import io.mockk.InternalPlatform.toStr
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MatchedCall
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallVerifier
import io.mockk.common.VerificationHelpers.formatCalls

open class UnorderedCallVerifier(val stubRepo: StubRepository) : CallVerifier {
    override fun verify(calls: List<MatchedCall>, min: Int, max: Int): MockKGateway.VerificationResult {
        for ((i, call) in calls.withIndex()) {
            val result = matchCall(call, min, max, "call ${i + 1} of ${calls.size}.")

            if (!result.matches) {
                return result
            }
        }
        return MockKGateway.VerificationResult(true)
    }

    private fun matchCall(call: MatchedCall, min: Int, max: Int, callIdxMsg: String): MockKGateway.VerificationResult {
        val stub = stubRepo.stubFor(call.invocation.self)
        val allCallsForMock = stub.allRecordedCalls()
        val allCallsForMockMethod = allCallsForMock.filter {
            call.matcher.method == it.method
        }
        val result = when (allCallsForMockMethod.size) {
            0 -> {
                if (min == 0 && max == 0) {
                    MockKGateway.VerificationResult(true)
                } else if (allCallsForMock.isEmpty()) {
                    MockKGateway.VerificationResult(false, "$callIdxMsg ${stub.toStr()}/${call.matcher.method.toStr()} was not called")
                } else {
                    MockKGateway.VerificationResult(false, "$callIdxMsg ${stub.toStr()}/${call.matcher.method.toStr()} was not called.\n" +
                            "Calls to same mock:\n" + formatCalls(allCallsForMock))
                }
            }
            1 -> {
                val onlyCall = allCallsForMockMethod.get(0)
                if (call.matcher.match(onlyCall)) {
                    if (1 in min..max) {
                        MockKGateway.VerificationResult(true)
                    } else {
                        MockKGateway.VerificationResult(false, "$callIdxMsg One matching call found, but needs at least $min${atMostMsg(max)} calls")
                    }
                } else {
                    MockKGateway.VerificationResult(false, "$callIdxMsg Only one matching call to ${stub.toStr()}/${call.matcher.method.toStr()} happened, but arguments are not matching:\n" +
                            describeArgumentDifference(call.matcher, onlyCall))
                }
            }
            else -> {
                val n = allCallsForMockMethod.filter { call.matcher.match(it) }.count()
                if (n in min..max) {
                    MockKGateway.VerificationResult(true)
                } else {
                    if (n == 0) {
                        MockKGateway.VerificationResult(false,
                                "$callIdxMsg No matching calls found.\n" +
                                        "Calls to same method:\n" + formatCalls(allCallsForMockMethod))
                    } else {
                        MockKGateway.VerificationResult(false,
                                "$callIdxMsg $n matching calls found, " +
                                        "but needs at least $min${atMostMsg(max)} calls")
                    }
                }
            }
        }
        return result
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