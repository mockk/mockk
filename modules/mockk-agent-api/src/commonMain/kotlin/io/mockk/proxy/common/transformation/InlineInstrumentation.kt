package io.mockk.proxy.common.transformation

interface InlineInstrumentation {
    fun execute(request: TransformationRequest): () -> Unit
}
