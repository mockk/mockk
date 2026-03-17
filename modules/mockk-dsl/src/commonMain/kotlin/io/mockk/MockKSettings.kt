package io.mockk

expect object MockKSettings {
    val relaxed: Boolean

    val relaxUnitFun: Boolean

    val recordPrivateCalls: Boolean

    val stackTracesOnVerify: Boolean

    val stackTracesAlignment: StackTracesAlignment

    val failOnSetBackingFieldException: Boolean

    fun setRelaxed(value: Boolean)

    fun setRelaxUnitFun(value: Boolean)

    fun setRecordPrivateCalls(value: Boolean)

    fun setStackTracesOnVerify(value: Boolean)

    fun setStackTracesAlignment(value: String)

    fun setFailOnSetBackingFieldException(value: Boolean)
}

enum class StackTracesAlignment {
    LEFT,
    CENTER,
}

fun stackTracesAlignmentValueOf(property: String): StackTracesAlignment =
    try {
        enumValueOf(property.uppercase())
    } catch (_: IllegalArgumentException) {
        StackTracesAlignment.CENTER
    }
