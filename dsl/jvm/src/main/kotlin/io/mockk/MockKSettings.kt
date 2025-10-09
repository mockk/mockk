package io.mockk

import java.util.*

actual object MockKSettings {
    private val properties = Properties()

    init {
        MockKSettings::class.java
            .getResourceAsStream("settings.properties")
            ?.use(properties::load)
    }

    private fun booleanProperty(property: String, defaultValue: String) =
        properties.getProperty(
            property,
            defaultValue
        )!!.toBoolean()


    actual val relaxed: Boolean
        get() = booleanProperty("relaxed", "false")

    actual val relaxUnitFun: Boolean
        get() = booleanProperty("relaxUnitFun", "false")

    actual val recordPrivateCalls: Boolean
        get() = booleanProperty("recordPrivateCalls", "false")


    fun setRelaxed(value: Boolean) {
        properties.setProperty("relaxed", value.toString());
    }

    fun setRelaxUnitFun(value: Boolean) {
        properties.setProperty("relaxUnitFun", value.toString());
    }

    fun setRecordPrivateCalls(value: Boolean) {
        properties.setProperty("recordPrivateCalls", value.toString());
    }
}