package io.mockk.impl.verify

import io.mockk.CapturingSlotMatcher
import io.mockk.Invocation
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
import io.mockk.impl.verify.VerificationHelpers.stackTraces

/**
 * Verifies that the total number of unique recorded calls matching any of the declared patterns
 * is within the requested bounds ([VerificationParameters.min]..[VerificationParameters.max]).
 *
 * An invocation matching several overlapping patterns is counted only once.
 */
open class AnyCallVerifier(
    val stubRepo: StubRepository,
    val safeToString: SafeToString,
) : CallVerifier {
    private val captureBlocks = mutableListOf<() -> Unit>()

    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters,
    ): VerificationResult {
        val min = params.min
        val max = params.max

        val matchedCalls = LinkedHashSet<Ref>()
        val patternDescriptions = mutableListOf<String>()
        for ((i, call) in verificationSequence.withIndex()) {
            val callIdxMsg = safeToString.exec { "call ${i + 1} of ${verificationSequence.size}: ${call.matcher}" }

            val matcher = call.matcher
            val stub = stubRepo.stubFor(matcher.self)
            val allCallsForMockMethod = stub.allRecordedCalls(matcher.method)
            val matchedCallsForPattern = allCallsForMockMethod.filter(matcher::match)

            if (matchedCallsForPattern.size > 1 && matcher.args.any { it is CapturingSlotMatcher<*> }) {
                val msg =
                    "$matcher execution is being verified more than once and its arguments are being " +
                        "captured with a slot.\n" +
                        "This will store only the argument of the last invocation in the slot.\n" +
                        "If you want to store all the arguments, use a mutableList to capture arguments."
                throw MockKException(msg)
            }

            matchedCallsForPattern.forEach { matchedCalls.add(InternalPlatform.ref(it)) }
            captureBlocks.add {
                for (matchedCall in matchedCallsForPattern) {
                    matcher.captureAnswer(matchedCall)
                }
            }
            patternDescriptions.add(callIdxMsg)
        }

        val allCallsForMocks =
            verificationSequence
                .map { it.matcher.self }
                .distinct()
                .flatMap { stubRepo.stubFor(it).allRecordedCalls() }

        val n = matchedCalls.size

        return if (min == 0 && max == 0) {
            if (n == 0) {
                VerificationResult.OK(listOf())
            } else {
                VerificationResult.Failure(
                    "None of the patterns should be called, " +
                        "but $n matching calls found" +
                        "\n\nCalls:\n" +
                        formatCalls(matchedCalls.map { it.value as Invocation }) +
                        "\n" +
                        if (MockKSettings.stackTracesOnVerify) {
                            "\n\nStack traces:\n" + stackTraces(matchedCalls.map { it.value as Invocation })
                        } else {
                            ""
                        },
                )
            }
        } else {
            if (n in min..max) {
                VerificationResult.OK(matchedCalls.map { it.value as Invocation })
            } else {
                VerificationResult.Failure(
                    safeToString.exec {
                        "$n matching calls found for the declared patterns: " +
                            "${patternDescriptions.joinToString(", ")}, " +
                            "but needs ${callsBoundsMsg(min, max)}" +
                            "\n\nCalls:\n" +
                            formatCalls(allCallsForMocks) +
                            "\n" +
                            if (MockKSettings.stackTracesOnVerify) {
                                "\n\nStack traces:\n" + stackTraces(allCallsForMocks)
                            } else {
                                ""
                            }
                    },
                )
            }
        }
    }

    override fun captureArguments() = captureBlocks.forEach { it() }

    private fun callsBoundsMsg(
        min: Int,
        max: Int,
    ): String =
        when {
            max == Int.MAX_VALUE -> "at least $min calls"
            min == max -> "exactly $min calls"
            else -> "at least $min and at most $max calls"
        }
}
