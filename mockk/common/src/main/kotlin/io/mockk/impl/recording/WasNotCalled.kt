package io.mockk.impl.recording

import io.mockk.MethodDescription

object WasNotCalled {
    val method = MethodDescription(
        "wasNot Called",
        Unit::class,
        true,
        false,
        false,
        Unit::class,
        listOf(),
        -1,
        false
    )
}