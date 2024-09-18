package io.mockk.impl.verify

import io.mockk.CapturingSlotMatcher
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.MockKException
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.MockKSettings
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

            when (
                val result = matchCall(call, min, max, callIdxMsg)
            ) {
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

        val matchedCalls = allCallsForMockMethod.filter(matcher::match)

        if (matchedCalls.size > 1 && matcher.args.any { it is CapturingSlotMatcher<*> }) {
            val msg = "$matcher execution is being verified more than once and its arguments are being captured with a slot.\n" +
                "This will store only the argument of the last invocation in the slot.\n" +
                "If you want to store all the arguments, use a mutableList to capture arguments."
            throw MockKException(msg)
        }

        val result = if (min == 0 && max == 0) {
            if (matchedCalls.isEmpty()) {
                VerificationResult.OK(listOf())
            } else {
                VerificationResult.Failure(
                    "$callIdxMsg should not be called" +
                            "\n\nCalls:\n" +
                            formatCalls(allCallsForMockMethod) +
                            "\n" +
                            if (MockKSettings.stackTracesOnVerify)
                                "\n\nStack traces:\n" + stackTraces(allCallsForMockMethod)
                            else
                                ""
                )
            }
        } else when (allCallsForMockMethod.size) {
            0 -> {
                if (min == 0) {
                    VerificationResult.OK(listOf())
                } else if (allCallsForMock.isEmpty()) {
                    VerificationResult.Failure("$callIdxMsg was not called")
                } else {
                    VerificationResult.Failure(safeToString.exec {
                        "$callIdxMsg was not called." +
                                "\n\nCalls to same mock:\n" +
                                formatCalls(allCallsForMock) +
                                "\n" +
                                if (MockKSettings.stackTracesOnVerify)
                                    "\n\nStack traces:\n" + stackTraces(allCallsForMock)
                                else
                                    ""
                    })
                }
            }
            1 -> {
                val onlyCall = allCallsForMockMethod[0]
                if (matchedCalls.size == 1) {
                    if (1 in min..max) {
                        VerificationResult.OK(listOf(onlyCall))
                    } else {
                        VerificationResult.Failure(
                            "$callIdxMsg. One matching call found, but needs ${callsBoundsMsg(min, max)}" +
                                    "\nCall: " + allCallsForMock.first() +
                                    if (MockKSettings.stackTracesOnVerify)
                                        "\nStack trace:\n" + stackTrace(0, allCallsForMock.first().callStack())
                                    else
                                        ""
                        )
                    }
                } else {
                    if (0 in min..max) {
                        VerificationResult.OK(listOf())
                    } else {
                        VerificationResult.Failure(safeToString.exec {
                            "$callIdxMsg. Only one matching call to ${stub.toStr()}/${matcher.method.toStr()} happened, but arguments are not matching:\n" +
                                describeArgumentDifference(matcher, onlyCall) +
                                if (MockKSettings.stackTracesOnVerify)
                                    "\nStack trace:\n" + stackTrace(0, allCallsForMock.first().callStack())
                                else
                                    ""
                        })
                    }
                }
            }
            else -> {
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
                                        "\n" +
                                        if (MockKSettings.stackTracesOnVerify)
                                            "\n\nStack traces:\n" + stackTraces(allCallsForMockMethod)
                                        else
                                            ""
                            })
                    } else {
                        VerificationResult.Failure(
                            "$callIdxMsg. $n matching calls found, but needs ${callsBoundsMsg(min, max)}" +
                                    "\nCalls:\n" +
                                    formatCalls(allCallsForMock) +
                                    "\n" +
                                    if (MockKSettings.stackTracesOnVerify)
                                        "\n\nStack traces:\n" + stackTraces(allCallsForMock)
                                    else
                                        ""
                        )
                    }
                }
            }
        }

        captureBlocks.add {
            for (call in matchedCalls) {
                matcher.captureAnswer(call)
            }
        }

        return result
    }

    override fun captureArguments() = captureBlocks.forEach { it() }

    private fun callsBoundsMsg(min: Int, max: Int): String {
        return when {
            max == Int.MAX_VALUE -> "at least $min calls"
            min == max -> "exactly $min calls"
            else -> "at least $min and at most $max calls"
        }
    }

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
