package io.mockk.impl

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKMatcherScope
import kotlinx.coroutines.experimental.runBlocking
import kotlin.reflect.KClass

internal open class CommonRecorder(val gateway: MockKGatewayImpl) {

    internal fun <T, S : MockKMatcherScope> record(scope: S,
                                                            mockBlock: (S.() -> T)?,
                                                            coMockBlock: (suspend S.() -> T)?) {
        try {
            val callRecorder = gateway.callRecorder

            val block: () -> T = if (mockBlock != null) {
                { scope.mockBlock() }
            } else if (coMockBlock != null) {
                { runBlocking { scope.coMockBlock() } }
            } else {
                { throw MockKException("You should specify either 'mockBlock' or 'coMockBlock'") }
            }

            var childTypes = mutableMapOf<Int, KClass<*>>()
            callRecorder.autoHint(childTypes,0, 64, block)
            val n = callRecorder.estimateCallRounds();
            for (i in 1 until n) {
                callRecorder.autoHint(childTypes, i, n, block)
            }
            callRecorder.catchArgs(n, n)

        } catch (ex: ClassCastException) {
            throw MockKException("Class cast exception. " +
                    "Probably type information was erased.\n" +
                    "In this case use `hint` before call to specify " +
                    "exact return type of a method. ", ex)
        }
    }

    private fun <T> MockKGateway.CallRecorder.autoHint(childTypes: MutableMap<Int, KClass<*>>, i: Int, n: Int, block: () -> T) {
        var callsPassed = -1
        while (true) {
            catchArgs(i, n)
            childTypes.forEach { callN, cls ->
                hintNextReturnType(cls, callN)
            }
            try {
                block()
                break
            } catch (ex: ClassCastException) {
                val clsName = extractClassName(ex) ?: throw ex
                val nCalls = nCalls()
                if (nCalls <= callsPassed) {
                    throw ex
                }
                callsPassed = nCalls
                val cls = Class.forName(clsName).kotlin

                log.trace { "Auto hint for $nCalls-th call: $cls" }
                childTypes[nCalls] = cls
            }
        }
    }

    internal fun prettifyCoroutinesException(ex: NoClassDefFoundError): Throwable {
        return if (ex.message?.contains("kotlinx/coroutines/") ?: false) {
            MockKException("Add coroutines support artifact 'org.jetbrains.kotlinx:kotlinx-coroutines-core' to your project ")
        } else {
            ex
        }
    }

    fun extractClassName(ex: ClassCastException): String? {
        return cannotBeCastRegex.find(ex.message!!)?.groups?.get(1)?.value
    }

    companion object {
        val cannotBeCastRegex = Regex("cannot be cast to (.+)$")
        val log = Logger<CommonRecorder>()
    }
}