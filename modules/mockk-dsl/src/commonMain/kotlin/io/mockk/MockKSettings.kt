package io.mockk

expect object MockKSettings {
    val relaxed: Boolean

    val relaxUnitFun: Boolean

    val recordPrivateCalls: Boolean

    val stackTracesOnVerify: Boolean

    val stackTracesAlignment: StackTracesAlignment

    val failOnSetBackingFieldException: Boolean
}

enum class StackTracesAlignment {
    LEFT,
    CENTER,
}

fun stackTracesAlignmentValueOf(property: String): StackTracesAlignment =
    try {
        enumValueOf(property.uppercase())
    } catch (e: IllegalArgumentException) {
        StackTracesAlignment.CENTER
    }
