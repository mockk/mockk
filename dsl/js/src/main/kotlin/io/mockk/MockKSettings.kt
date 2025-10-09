package io.mockk

actual object MockKSettings {
    actual val relaxed: Boolean
        get() = js("global.io_mockk_settings_relaxed || false") as Boolean

    actual val relaxUnitFun: Boolean
        get() = js("global.io_mockk_settings_relaxUnitFun || false") as Boolean

    actual val recordPrivateCalls: Boolean
        get() = js("global.io_mockk_settings_recordPrivateCalls || false") as Boolean
}