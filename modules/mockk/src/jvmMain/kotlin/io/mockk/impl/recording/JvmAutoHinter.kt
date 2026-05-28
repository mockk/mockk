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
        block: () -> T,
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

    private fun extractClassName(ex: ClassCastException): String =
        ex.message?.let {
            exceptionMessage
                .find(it)
                ?.groups
                ?.get(3)
                ?.value
        } ?: throw ex

    companion object {
        // HotSpot pre-JEP358 (JDK 8-13):
        //   net.bytebuddy.renamed.java.lang.Object$ByteBuddy$As29nsJf$ByteBuddy$877l7O7D cannot be cast to io.mockk.impl.recording.states.CallRecordingState
        // HotSpot JEP358 (JDK 14+):
        //   class net.bytebuddy.renamed.java.lang.Object$ByteBuddy$rpycQEYo$ByteBuddy$bHEk1ADY cannot be cast to class java.lang.String (net.bytebuddy.renamed.java.lang.Object$ByteBuddy$rpycQEYo$ByteBuddy$bHEk1ADY is in unnamed module of loader net.bytebuddy.dynamic.loading.ByteArrayClassLoader @19569ebd; java.lang.String is in module java.base of loader 'bootstrap')
        // OpenJ9 / IBM Semeru (all releases):
        //   net.bytebuddy.renamed.java.lang.Object$ByteBuddy$As29nsJf$ByteBuddy$877l7O7D incompatible with java.lang.String
        val exceptionMessage =
            Regex("(?:cannot be cast to|incompatible with) (class )?(.+/)?(.+?)( \\((.+)\\))?$")

        val log = Logger<JvmAutoHinter>()
    }
}
