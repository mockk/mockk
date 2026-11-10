package io.mockk.impl.verify

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKGateway.CallVerifier
import io.mockk.MockKGateway.ConstructorMockFactory
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKGateway.VerificationResult
import io.mockk.MockKSettings
import io.mockk.RecordedCall
import io.mockk.impl.log.SafeToString
import io.mockk.impl.stub.MockKStub
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.VerificationHelpers.formatCalls
import io.mockk.impl.verify.VerificationHelpers.stackTraces
import kotlin.reflect.KClass

/**
 * Verifies a bare `anyConstructed<T>()` placeholder as an aggregate verification target.
 *
 * The placeholder records no call of its own, so the regular call verifiers have nothing
 * to match it against. Instead this verifier collects every call recorded on the
 * representative mocks that currently stand for constructed instances of the placeholder
 * types and checks their total number against the requested bounds.
 *
 * Only `exactly = 0` bounds are currently supported: verifying that no constructed
 * instance of `T` was used at all. Cross-instance semantics for other bounds (for example
 * `exactly = 1` spread over several constructed instances) are intentionally left open
 * (see issue #1224).
 */
open class ConstructorAggregateVerifier(
    val stubRepo: StubRepository,
    val constructorMockFactory: ConstructorMockFactory,
    val safeToString: SafeToString,
    val placeholders: List<Any>,
) : CallVerifier {
    override fun verify(
        verificationSequence: List<RecordedCall>,
        params: VerificationParameters,
    ): VerificationResult {
        val min = params.min
        val max = params.max

        if (min != 0 || max != 0) {
            return VerificationResult.Failure(
                "A bare anyConstructed<T>() placeholder can only be verified with exactly = 0, " +
                    "but bounds ${callsBoundsMsg(min, max)} were requested",
            )
        }

        val placeholderTypes = requestedTypes()

        val allCalls =
            placeholderTypes
                .flatMap { constructorMockFactory.representativeMocks(it) }
                .distinct()
                .map { stubRepo.stubFor(it) }
                .flatMap { it.allRecordedCalls() }

        return if (allCalls.isEmpty()) {
            VerificationResult.OK(listOf())
        } else {
            VerificationResult.Failure(
                safeToString.exec {
                    "Expected no calls on constructed instances of " +
                        placeholderTypes.joinToString(", ") { it.toStr() } +
                        ", but ${allCalls.size} were found" +
                        "\n\nCalls:\n" +
                        formatCalls(allCalls) +
                        "\n" +
                        if (MockKSettings.stackTracesOnVerify) {
                            "\n\nStack traces:\n" + stackTraces(allCalls)
                        } else {
                            ""
                        }
                },
            )
        }
    }

    override fun captureArguments() = Unit

    private fun requestedTypes(): List<KClass<*>> =
        placeholders
            .mapNotNull { stubRepo[it] as? MockKStub }
            .map { it.type }
            .distinct()

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
