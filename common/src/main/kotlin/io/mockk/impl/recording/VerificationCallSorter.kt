package io.mockk.impl.recording

import io.mockk.RecordedCall
import io.mockk.impl.InternalPlatform

class VerificationCallSorter {
    lateinit var wasNotCalledCalls: List<RecordedCall>
    lateinit var regularCalls: List<RecordedCall>

    fun sort(calls: List<RecordedCall>) {
        wasNotCalledCalls = calls.filter {
            it.matcher.method == WasNotCalled.method
        }

        val callSet = calls.map {
            InternalPlatform.ref(it)
        }.toMutableList()

        tailrec fun removeChain(call: RecordedCall) {
            callSet.remove(InternalPlatform.ref(call))

            val selfChain = call.selfChain
            if (selfChain != null) {
                removeChain(selfChain)
            }
        }

        wasNotCalledCalls.forEach(::removeChain)
        regularCalls = callSet.map { it.value as RecordedCall }.toList()
    }
}