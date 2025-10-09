package io.mockk

expect object MockKSettings {
    val relaxed: Boolean

    val relaxUnitFun: Boolean

    val recordPrivateCalls: Boolean
}