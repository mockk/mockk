package io.mockk.proxy.android.transformation

import io.mockk.agent.MockKAgentLogger
import io.mockk.proxy.android.JvmtiAgent

internal class InlineInstrumentation(
    private val log: MockKAgentLogger,
    private val specMap: ClassTransformationSpecMap,
    private val agent: JvmtiAgent
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
                agent.requestTransformClasses(classes)
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
                agent.requestTransformClasses(classes)
            }
        } catch (ex: Exception) {
            log.warn(ex, "Failed to revert class transformation ${reverseInstrumentationRequest.classes}")
        }
    }
}
