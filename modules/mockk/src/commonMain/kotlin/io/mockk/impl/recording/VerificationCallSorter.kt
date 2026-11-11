package io.mockk.impl.recording

import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform

class VerificationCallSorter {
    /**
     * Markers of the form `mock wasNot Called`, verified by checking the recorded calls of
     * the mock itself.
     */
    lateinit var wasNotCalledCalls: List<RecordedCall>

    /**
     * Call patterns of the form `mock.method(args) wasNot Called`: the receiver of `wasNot`
     * was not a mock, but the result of a call recorded inside the verify block. The
     * producing call pattern must not have been invoked on the mock (issue #1290).
     */
    lateinit var wasNotCalledCallResults: List<RecordedCall>

    lateinit var regularCalls: List<RecordedCall>

    fun sort(calls: List<RecordedCall>) {
        val markers =
            calls.filter {
                it.matcher.method == WasNotCalled.method
            }

        val producingCalls = markers.map { marker -> marker.producingCall(calls) }

        val callSet =
            calls
                .map {
                    InternalPlatform.ref(it)
                }.toMutableList()

        tailrec fun removeChain(call: RecordedCall) {
            callSet.remove(InternalPlatform.ref(call))

            val selfChain = call.selfChain
            if (selfChain != null) {
                removeChain(selfChain)
            }
        }

        for (marker in markers) {
            removeChain(marker)
        }
        // For child-mock receivers the chain is already followed through marker.selfChain,
        // so removing it again is a no-op there; for value receivers it removes the
        // producing call, which must not be verified as a regular call.
        producingCalls.filterNotNull().forEach(::removeChain)

        wasNotCalledCalls =
            markers
                .zip(producingCalls)
                .filter { it.second == null }
                .map { it.first }
        wasNotCalledCallResults = producingCalls.filterNotNull()
        regularCalls = callSet.map { it.value as RecordedCall }.toList()
    }

    /**
     * The call that produced the receiver of a `wasNot Called` marker.
     *
     * When the method returns a mockable type, the receiver is a temporary mock that
     * [PermanentMocker] replaces with a child mock and links via [RecordedCall.selfChain].
     * For non-mockable return types (Unit, String, primitives, ...) the receiver is a plain
     * value with no chain: it is then resolved by identity, because the receiver handed to
     * `wasNot` is exactly the return value the recorder returned to the verify block, so it
     * is the same instance stored as the [RecordedCall.retValue] of the call evaluated
     * right before the marker.
     *
     * Returns null for markers that do not follow a recorded call (e.g. `mock wasNot Called`
     * or a receiver that is not linked to anything recorded in the block).
     */
    private fun RecordedCall.producingCall(calls: List<RecordedCall>): RecordedCall? {
        if (selfChain != null) {
            return selfChain
        }

        val markerIndex = calls.indexOfFirst { it === this }
        if (markerIndex <= 0) {
            return null
        }

        val candidate = calls[markerIndex - 1]
        if (candidate.matcher.method == WasNotCalled.method) {
            return null
        }

        // For void methods the JVM discards the recorded return value, so the block
        // evaluates the expression to the `Unit` singleton instead of the recorded
        // [RecordedCall.retValue].
        return candidate.takeIf {
            it.retValue === matcher.self || (it.matcher.method.returnsUnit && matcher.self === Unit)
        }
    }
}
