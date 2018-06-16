package io.mockk.impl.verify

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationResult
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeLog
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.formatCalls
import io.mockk.impl.verify.VerificationHelpers.stackTrace
import io.mockk.impl.verify.VerificationHelpers.stackTraces

open class UnorderedCallVerifier(
    val stubRepo: StubRepository,
    val safeLog: SafeLog
) : CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(verificationSequence: List<RecordedCall>, min: Int, max: Int): VerificationResult {
        for ((i, call) in verificationSequence.withIndex()) {
            val callIdxMsg = safeLog.exec { "call ${i + 1} of ${verificationSequence.size}: ${call.matcher}" }
            val result = matchCall(call, min, max, callIdxMsg)

            if (!result.matches) {
                return result
            }
        }
        return VerificationResult(true)
    }

    private fun matchCall(recordedCall: RecordedCall, min: Int, max: Int, callIdxMsg: String): VerificationResult {
        val stub = stubRepo.stubFor(recordedCall.matcher.self)
        val allCallsForMock = stub.allRecordedCalls()
        val allCallsForMockMethod = allCallsForMock.filter {
            recordedCall.matcher.method == it.method
        }


        val result = if (min == 0 && max == 0) {
            val n = allCallsForMockMethod.filter { recordedCall.matcher.match(it) }.count()
            if (n == 0) {
                VerificationResult(true)
            } else {
                VerificationResult(
                    false, "$callIdxMsg should not be called" +
                            "\n\nCalls:\n" +
                            formatCalls(allCallsForMockMethod) +
                            "\n\nStack traces:\n" +
                            stackTraces(allCallsForMockMethod)
                )
            }
        } else when (allCallsForMockMethod.size) {
            0 -> {
                if (min == 0 && max == 0) {
                    VerificationResult(true)
                } else if (allCallsForMock.isEmpty()) {
                    VerificationResult(false, "$callIdxMsg was not called")
                } else {
                    VerificationResult(false, safeLog.exec {
                        "$callIdxMsg was not called." +
                                "\n\nCalls to same mock:\n" +
                                formatCalls(allCallsForMock) +
                                "\n\nStack traces:\n" +
                                stackTraces(allCallsForMock)
                    })
                }
            }
            1 -> {
                val onlyCall = allCallsForMockMethod.get(0)
                if (recordedCall.matcher.match(onlyCall)) {
                    if (1 in min..max) {
                        VerificationResult(true)
                    } else {
                        VerificationResult(
                            false,
                            "$callIdxMsg. One matching call found, but needs at least $min${atMostMsg(max)} calls" +
                                    "\nCall: " + allCallsForMock.first() +
                                    "\nStack trace:\n" +
                                    stackTrace(0, allCallsForMock.first().callStack())

                        )
                    }
                } else {
                    VerificationResult(false, safeLog.exec {
                        "$callIdxMsg. Only one matching call to ${stub.toStr()}/${recordedCall.matcher.method.toStr()} happened, but arguments are not matching:\n" +
                                describeArgumentDifference(recordedCall.matcher, onlyCall) +
                                "\nStack trace:\n" +
                                stackTrace(0, allCallsForMock.first().callStack())
                    })
                }
            }
            else -> {
                val n = allCallsForMockMethod.filter { recordedCall.matcher.match(it) }.count()
                if (n in min..max) {
                    VerificationResult(true)
                } else {
                    if (n == 0) {
                        VerificationResult(false,
                            safeLog.exec {
                                "$callIdxMsg. No matching calls found." +
                                        "\n\nCalls to same method:\n" +
                                        formatCalls(allCallsForMockMethod) +
                                        "\n\nStack traces:\n" +
                                        stackTraces(allCallsForMockMethod)
                            })
                    } else {
                        VerificationResult(
                            false,
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

        captureBlocks.add({
            for (call in allCallsForMockMethod) {
                recordedCall.matcher.captureAnswer(call)
            }
        })

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
            str.append("[$i]: argument: $arg, matcher: $argMatcher, result: ${if (matches) "+" else "-"}\n")
        }
        return str.toString()
    }
}