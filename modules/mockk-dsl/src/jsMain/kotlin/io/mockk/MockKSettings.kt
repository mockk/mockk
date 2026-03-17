package io.mockk

actual object MockKSettings {
    actual val relaxed: Boolean
        get() = js("global.io_mockk_settings_relaxed || false") as Boolean

    actual val relaxUnitFun: Boolean
        get() = js("global.io_mockk_settings_relaxUnitFun || false") as Boolean

    actual val recordPrivateCalls: Boolean
        get() = js("global.io_mockk_settings_recordPrivateCalls || false") as Boolean

    actual val stackTracesOnVerify: Boolean
        get() = js("global.io_mockk_settings_stackTracesOnVerify || false") as Boolean

    actual val stackTracesAlignment: StackTracesAlignment
        get() =
            stackTracesAlignmentValueOf(
                js("global.io_mockk_settings_stackTracesAlignment || \"center\"") as String,
            )

    actual val failOnSetBackingFieldException: Boolean
        get() = js("global.io_mockk_settings_failOnSetBackingFieldException || false") as Boolean

    actual fun setRelaxed(value: Boolean) {
        js("global.io_mockk_settings_relaxed = value")
    }

    actual fun setRelaxUnitFun(value: Boolean) {
        js("global.io_mockk_settings_relaxUnitFun = value")
    }

    actual fun setRecordPrivateCalls(value: Boolean) {
        js("global.io_mockk_settings_recordPrivateCalls = value")
    }

    actual fun setStackTracesOnVerify(value: Boolean) {
        js("global.io_mockk_settings_stackTracesOnVerify = value")
    }

    actual fun setStackTracesAlignment(value: String) {
        js("global.io_mockk_settings_stackTracesAlignment = value")
    }

    actual fun setFailOnSetBackingFieldException(value: Boolean) {
        js("global.io_mockk_settings_failOnSetBackingFieldException = value")
    }
}
