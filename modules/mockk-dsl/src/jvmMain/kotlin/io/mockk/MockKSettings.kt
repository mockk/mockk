package io.mockk

import io.mockk.core.config.UnifiedPropertiesLoader

actual object MockKSettings {
    private val properties = UnifiedPropertiesLoader.loadProperties()

    private fun booleanProperty(
        property: String,
        defaultValue: String,
    ) = properties
        .getProperty(
            property,
            defaultValue,
        )!!
        .toBoolean()

    actual val relaxed: Boolean
        get() = booleanProperty("relaxed", "false")

    actual val relaxUnitFun: Boolean
        get() = booleanProperty("relaxUnitFun", "false")

    actual val recordPrivateCalls: Boolean
        get() = booleanProperty("recordPrivateCalls", "false")

    actual val stackTracesOnVerify: Boolean
        get() = booleanProperty("stackTracesOnVerify", "true")

    actual val stackTracesAlignment: StackTracesAlignment
        get() = stackTracesAlignmentValueOf(properties.getProperty("stackTracesAlignment", "center"))

    actual val failOnSetBackingFieldException: Boolean
        get() = booleanProperty("failOnSetBackingFieldException", "false")

    actual fun setRelaxed(value: Boolean) {
        properties.setProperty("relaxed", value.toString())
    }

    actual fun setRelaxUnitFun(value: Boolean) {
        properties.setProperty("relaxUnitFun", value.toString())
    }

    actual fun setRecordPrivateCalls(value: Boolean) {
        properties.setProperty("recordPrivateCalls", value.toString())
    }

    actual fun setStackTracesOnVerify(value: Boolean) {
        properties.setProperty("stackTracesOnVerify", value.toString())
    }

    actual fun setStackTracesAlignment(value: String) {
        properties.setProperty("stackTracesAlignment", value)
    }

    actual fun setFailOnSetBackingFieldException(value: Boolean) {
        properties.setProperty("failOnSetBackingFieldException", value.toString())
    }
}
