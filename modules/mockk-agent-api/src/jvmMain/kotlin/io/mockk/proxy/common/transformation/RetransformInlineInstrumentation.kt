package io.mockk.proxy.common.transformation

import io.mockk.proxy.MockKAgentLogger
import java.lang.instrument.UnmodifiableClassException

abstract class RetransformInlineInstrumentation(
    protected val log: MockKAgentLogger,
    private val specMap: ClassTransformationSpecMap
) : InlineInstrumentation {

    protected abstract fun retransform(classesToTransform: Collection<Class<*>>)

    override fun execute(request: TransformationRequest): () -> Unit {
        var cancellation: (() -> Unit)? = null
        try {
            specMap.applyTransformation(request) {
                cancellation = { doCancel(request) }

                retransform(it.classes)
            }
        } catch (ex: java.lang.Exception) {
            log.warn(ex, "Failed to transform classes ${request.classes}")
            cancellation?.invoke()
            return {}
        }

        return cancellation ?: {}
    }

    private fun doCancel(request: TransformationRequest) {
        try {
            specMap.applyTransformation(
                request.reverse()
            ) {
                val classes = it.classes
                if (classes.isNotEmpty()) {
                    log.trace("Retransforming back ${it.classes}")
                    retransform(classes)
                }
            }
        } catch (ex: UnmodifiableClassException) {
            log.warn(ex, "Failed to revert class transformation ${request.classes}")
        }
    }
}