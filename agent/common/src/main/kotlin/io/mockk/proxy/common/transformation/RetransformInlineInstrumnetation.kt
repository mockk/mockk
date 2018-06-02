package io.mockk.proxy.common.transformation

import io.mockk.proxy.MockKAgentLogger

abstract class RetransformInlineInstrumnetation(
    private val log: MockKAgentLogger,
    private val specMap: ClassTransformationSpecMap
) : InlineInstrumentation {

    protected abstract fun retransform(classes: Array<Class<*>>)

    override fun execute(request: TransformationRequest): () -> Unit {
        val instrumentationRequest = specMap.applyTransformationRequest(
            request
        )

        val cancellation = { doCancel(request) }

        try {
            val classes = instrumentationRequest.classes.toTypedArray()
            if (classes.isNotEmpty()) {
                log.trace("Retransforming ${specMap.transformationMap(instrumentationRequest)}")
                retransform(classes)
            }
        } catch (ex: Exception) {
            log.warn(ex, "Failed to transform classes $instrumentationRequest")
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
                log.trace("Retransforming back ${specMap.transformationMap(reverseInstrumentationRequest)}")
                retransform(classes)
            }
        } catch (ex: Exception) {
            log.warn(ex, "Failed to revert class transformation ${reverseInstrumentationRequest.classes}")
        }
    }
}