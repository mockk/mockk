package io.mockk.impl.recording

import io.mockk.MockKGateway
import io.mockk.impl.log.Logger
import kotlin.reflect.KClass

class JvmAutoHinter : AutoHinter() {
    val childTypes = mutableMapOf<Int, KClass<*>>()

    override fun <T> autoHint(
        callRecorder: MockKGateway.CallRecorder,
        i: Int,
        n: Int,
        block: () -> T
    ) {
        var callsPassed = -1
        while (true) {
            callRecorder.round(i, n)
            childTypes.forEach { (callN, cls) ->
                callRecorder.hintNextReturnType(cls, callN)
            }
            try {
                block()
                break
            } catch (ex: ClassCastException) {
                val clsName = extractClassName(ex) ?: throw ex
                val nCalls = callRecorder.nCalls()
                if (nCalls <= callsPassed) {
                    throw ex
                }

                callRecorder.discardLastCallRound()

                callsPassed = nCalls
                val cls = Class.forName(clsName).kotlin

                log.trace { "Auto hint for $nCalls-th call: $cls" }
                childTypes[nCalls] = cls
            }
        }
    }

    fun extractClassName(ex: ClassCastException): String? {
        return cannotBeCastRegex.find(ex.message!!)?.groups?.get(1)?.value
    }

    companion object {
        val cannotBeCastRegex = Regex("cannot be cast to (.+)$")
        val log = Logger<JvmAutoHinter>()
    }
}