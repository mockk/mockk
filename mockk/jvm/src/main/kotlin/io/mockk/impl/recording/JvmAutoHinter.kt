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
                val clsName = extractClassName(ex)
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

    private fun extractClassName(ex: ClassCastException): String {
        return ex.message?.let {
            exceptionMessage.find(it)?.groups?.get(3)?.value
        } ?: throw ex
    }

    companion object {
        // JDK 8: net.bytebuddy.renamed.java.lang.Object$ByteBuddy$As29nsJf$ByteBuddy$877l7O7D cannot be cast to io.mockk.impl.recording.states.CallRecordingState
        // JDK 9:
        // JDK 10:
        // JDK 11: class net.bytebuddy.renamed.java.lang.Object$ByteBuddy$rpycQEYo$ByteBuddy$bHEk1ADY cannot be cast to class java.lang.String (net.bytebuddy.renamed.java.lang.Object$ByteBuddy$rpycQEYo$ByteBuddy$bHEk1ADY is in unnamed module of loader net.bytebuddy.dynamic.loading.ByteArrayClassLoader @19569ebd; java.lang.String is in module java.base of loader 'bootstrap')
        val exceptionMessage = Regex("cannot be cast to (class )?(.+/)?(.+?)( \\((.+)\\))?$")

        val log = Logger<JvmAutoHinter>()
    }
}