package io.mockk.impl.verify

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MockKGateway.*
import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform
import io.mockk.impl.Ref
import io.mockk.impl.log.SafeToString
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.formatCalls
import io.mockk.impl.verify.VerificationHelpers.stackTrace
import io.mockk.impl.verify.VerificationHelpers.stackTraces

open class UnorderedCallVerifier(
    val stubRepo: StubRepository,
    val safeToString: SafeToString
) : CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters
    ): VerificationResult {

        val min = params.min
        val max = params.max

        val verifiedCalls = mutableSetOf<Ref>()
        for ((i, call) in verificationSequence.withIndex()) {
            val callIdxMsg = safeToString.exec { "call ${i + 1} of ${verificationSequence.size}: ${call.matcher}" }
            val result = matchCall(call, min, max, callIdxMsg)

            when (result) {
                is VerificationResult.OK -> verifiedCalls.addAll(
                    result
                        .verifiedCalls
                        .map { InternalPlatform.ref(it) }
                )
                is VerificationResult.Failure -> return result
            }
        }
        return VerificationResult.OK(verifiedCalls.map { it.value as Invocation })
    }

    private fun matchCall(recordedCall: RecordedCall, min: Int, max: Int, callIdxMsg: String): VerificationResult {
        val matcher = recordedCall.matcher
        val stub = stubRepo.stubFor(matcher.self)
        val allCallsForMock = stub.allRecordedCalls()
        val allCallsForMockMethod = stub.allRecordedCalls(matcher.method)

        val result = if (min == 0 && max == 0) {
            if (!allCallsForMockMethod.any(matcher::match)) {
                VerificationResult.OK(listOf())
            } else {
                VerificationResult.Failure(
                    "$callIdxMsg should not be called" +
                            "\n\nCalls:\n" +
                            formatCalls(allCallsForMockMethod) +
                            "\n\nStack traces:\n" +
                            stackTraces(allCallsForMockMethod)
                )
            }
        } else when (allCallsForMockMethod.size) {
            0 -> {
                if (min == 0 && max == 0) {
                    VerificationResult.OK(listOf())
                } else if (allCallsForMock.isEmpty()) {
                    VerificationResult.Failure("$callIdxMsg was not called")
                } else {
                    VerificationResult.Failure(safeToString.exec {
                        "$callIdxMsg was not called." +
                                "\n\nCalls to same mock:\n" +
                                formatCalls(allCallsForMock) +
                                "\n\nStack traces:\n" +
                                stackTraces(allCallsForMock)
                    })
                }
            }
            1 -> {
                val onlyCall = allCallsForMockMethod[0]
                if (matcher.match(onlyCall)) {
                    if (1 in min..max) {
                        VerificationResult.OK(listOf(onlyCall))
                    } else {
                        VerificationResult.Failure(
                            "$callIdxMsg. One matching call found, but needs at least $min${atMostMsg(max)} calls" +
                                    "\nCall: " + allCallsForMock.first() +
                                    "\nStack trace:\n" +
                                    stackTrace(0, allCallsForMock.first().callStack())

                        )
                    }
                } else {
                    VerificationResult.Failure(safeToString.exec {
                        "$callIdxMsg. Only one matching call to ${stub.toStr()}/${matcher.method.toStr()} happened, but arguments are not matching:\n" +
                                describeArgumentDifference(matcher, onlyCall) +
                                "\nStack trace:\n" +
                                stackTrace(0, allCallsForMock.first().callStack())
                    })
                }
            }
            else -> {
                val matchedCalls = allCallsForMockMethod.filter(matcher::match)
                val n = matchedCalls.count()
                if (n in min..max) {
                    VerificationResult.OK(matchedCalls)
                } else {
                    if (n == 0) {
                        VerificationResult.Failure(
                            safeToString.exec {
                                "$callIdxMsg. No matching calls found." +
                                        "\n\nCalls to same method:\n" +
                                        formatCalls(allCallsForMockMethod) +
                                        "\n\nStack traces:\n" +
                                        stackTraces(allCallsForMockMethod)
                            })
                    } else {
                        VerificationResult.Failure(
                            "$callIdxMsg. $n matching calls found, " +
                                    "but needs at least $min${atMostMsg(max)} calls" +
                                    "\nCalls:\n" +
                                    formatCalls(allCallsForMock) +
                                    "\n\nStack traces:\n" +
                                    stackTraces(allCallsForMock)
                        )
                    }
                }
            }
        }

        captureBlocks.add {
            for (call in allCallsForMockMethod) {
                matcher.captureAnswer(call)
            }
        }

        return result
    }

    override fun captureArguments() {
        captureBlocks.forEach { it() }
    }

    private fun atMostMsg(max: Int) = if (max == Int.MAX_VALUE) "" else " and at most $max"

    private fun describeArgumentDifference(
        matcher: InvocationMatcher,
        invocation: Invocation
    ): String {
        val str = StringBuilder()
        for ((i, arg) in invocation.args.withIndex()) {
            val argMatcher = matcher.args[i]
            val matches = argMatcher.match(arg)
            val argStr = safeToString.exec { arg.toStr() }
            str.append("[$i]: argument: $argStr, matcher: $argMatcher, result: ${if (matches) "+" else "-"}\n")
        }
        return str.toString()
    }
}