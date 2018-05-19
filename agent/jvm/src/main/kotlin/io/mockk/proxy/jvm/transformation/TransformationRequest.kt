package io.mockk.proxy.jvm.transformation

data class TransformationRequest(
    val classes: Set<Class<*>>,
    val type: TransformationType,
    val untransform: Boolean = false
) {
    fun reverse() = TransformationRequest(
        classes,
        type,
        !untransform
    )
}