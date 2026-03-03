package io.mockk.impl.verify

import io.mockk.MethodDescription
import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.impl.InternalPlatform
import io.mockk.impl.JvmMockKGateway
import io.mockk.impl.instantiation.JvmMockFactoryHelper
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * Strategy seam for resolving a function reference to the recorded method signature.
 */
internal interface FunctionSignatureResolver {
    fun resolve(function: KFunction<*>): MethodDescription
}

/**
 * Default JVM resolver based on Java method metadata.
 */
internal object JavaMethodFunctionSignatureResolver : FunctionSignatureResolver {
    override fun resolve(function: KFunction<*>): MethodDescription =
        with(JvmMockFactoryHelper) {
            val method =
                checkNotNull(function.javaMethod) {
                    "$function has no JVM method metadata for top-level/static function confirmation"
                }
            method.toDescription()
        }
}

/**
 * JVM-only function-scoped verifier for statically mocked callable references.
 *
 * Design note:
 * Signature resolution is delegated to [FunctionSignatureResolver], so resolution logic can
 * evolve without changing the verification flow.
 */
internal class FunctionScopedVerificationAcknowledger(
    private val signatureResolver: FunctionSignatureResolver = JavaMethodFunctionSignatureResolver,
) {
    /**
     * Verifies that all recorded calls for [functions] were already verified.
     *
     * @param clear if `true`, recorded calls and verification marks are cleared only for
     * the selected function signatures after successful confirmation.
     * @throws MockKException if the corresponding static stub was not found.
     * @throws AssertionError if any recorded call for the selected functions is not verified.
     */
    fun confirmVerified(
        functions: Array<out KFunction<*>>,
        clear: Boolean = false,
    ) {
        // Defensive guard for internal callers. Public JVM overloads require at least one reference.
        if (functions.isEmpty()) return

        val implementation = MockKGateway.implementation()
        val gateway =
            implementation as? JvmMockKGateway
                ?: throw MockKException("Function-scoped confirmVerified is only supported for JVM implementation")

        val methodsByDeclaringClass =
            functions
                .map(signatureResolver::resolve)
                .distinct()
                .groupBy { it.declaringClass }

        methodsByDeclaringClass.forEach { (declaringClass, methods) ->
            val stub = gateway.stubRepo.stubFor(declaringClass.java)
            val methodSet = methods.toSet()

            val allCalls = stub.allRecordedCalls().filter { it.method in methodSet }
            val verifiedCalls = stub.verifiedCalls().filter { it.method in methodSet }
            val allCallRefs = allCalls.map { InternalPlatform.ref(it) }.toHashSet() // as hashSet for search optimization
            val verifiedCallRefs = verifiedCalls.map { InternalPlatform.ref(it) }.toHashSet() // for search optimization

            if (allCallRefs == verifiedCallRefs) return@forEach

            val notVerified = allCalls.filter { InternalPlatform.ref(it) !in verifiedCallRefs }
            val nonVerifiedReport =
                VerificationReportFormatter.reportNotVerified(
                    allCalls.size,
                    verifiedCalls.size,
                    notVerified,
                )
            throw AssertionError("Verification acknowledgment failed$nonVerifiedReport")
        }

        if (clear) {
            methodsByDeclaringClass.forEach { (declaringClass, methods) ->
                val stub = gateway.stubRepo.stubFor(declaringClass.java)
                val methodSet = methods.toSet()
                val allRecordedCalls = stub.allRecordedCalls()
                val verifiedCallRefs = stub.verifiedCalls().map { InternalPlatform.ref(it) }.toHashSet()

                val retainedCalls = allRecordedCalls.filter { it.method !in methodSet }
                val retainedVerifiedRefs =
                    retainedCalls
                        .map { InternalPlatform.ref(it) }
                        .filter { it in verifiedCallRefs }
                        .toHashSet()

                stub.clear(
                    MockKGateway.ClearOptions(
                        answers = false,
                        recordedCalls = true,
                        childMocks = false,
                        verificationMarks = true,
                        exclusionRules = false,
                    ),
                )

                // Since `stub.clear(...)` deletes the full call history for this static stub.
                // Calls from other functions are added back in the same stub, so `clear = true`
                // removes only the selected function signatures.
                retainedCalls.forEach { stub.recordCall(it) }
                retainedCalls.forEach {
                    // Restore the verified state for the calls we kept.
                    // Without this, calls that were already verified would become unverified again.
                    if (InternalPlatform.ref(it) in retainedVerifiedRefs) {
                        stub.markCallVerified(it)
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible facade used by call sites.
 */
internal object JvmFunctionScopedVerificationAcknowledger {
    private val delegate = FunctionScopedVerificationAcknowledger()

    fun confirmVerified(
        functions: Array<out KFunction<*>>,
        clear: Boolean = false,
    ) {
        delegate.confirmVerified(functions, clear)
    }
}
