package io.mockk.impl

import io.mockk.MockKGateway
import kotlin.reflect.KClass

open class AutoHinter {
    open fun <T> autoHint(callRecorder: MockKGateway.CallRecorder,
                          i: Int,
                          n: Int,
                          block: () -> T) {
        callRecorder.catchArgs(i, n)
        block()
    }
}