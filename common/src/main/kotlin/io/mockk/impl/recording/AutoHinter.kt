package io.mockk.impl.recording

import io.mockk.MockKGateway

open class AutoHinter {
    open fun <T> autoHint(
        callRecorder: MockKGateway.CallRecorder,
        i: Int,
        n: Int,
        block: () -> T
    ) {
        callRecorder.round(i, n)
        block()
    }
}