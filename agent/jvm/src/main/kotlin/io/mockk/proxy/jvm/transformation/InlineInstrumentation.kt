package io.mockk.proxy.jvm.transformation

import io.mockk.proxy.MockKAgentLogger
import io.mockk.proxy.common.transformation.ClassTransformationSpecMap
import io.mockk.proxy.common.transformation.TransformationRequest
import java.lang.instrument.Instrumentation
import java.lang.instrument.UnmodifiableClassException

internal class InlineInstrumentation(
    private val log: MockKAgentLogger,
    private val specMap: ClassTransformationSpecMap,
    private val instrumentation: Instrumentation
) {

    fun execute(request: TransformationRequest): () -> Unit {
        val instrumentationRequest = specMap.applyTransformationRequest(
            request
        )

        val cancellation = { doCancel(request) }

        try {
            val classes = instrumentationRequest.classes.toTypedArray()
            if (classes.isNotEmpty()) {
                log.trace("Retransforming ${instrumentationRequest.classes}")
                instrumentation.retransformClasses(*classes)
            }
        } catch (ex: Exception) {
            log.warn(ex, "Failed to transform classes ${instrumentationRequest.classes}")
            cancellation()
            return {}
        }

        return cancellation
    }

    private fun doCancel(request: TransformationRequest) {
        val reverseInstrumentationRequest =
            specMap.applyTransformationRequest(
                request.reverse()
            )

        try {
            val classes = reverseInstrumentationRequest.classes.toTypedArray()
            if (classes.isNotEmpty()) {
                log.trace("Retransforming back ${reverseInstrumentationRequest.classes}")
                instrumentation.retransformClasses(*classes)
            }
        } catch (ex: UnmodifiableClassException) {
            log.warn(ex, "Failed to revert class transformation ${reverseInstrumentationRequest.classes}")
        }
    }
}
