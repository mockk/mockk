package io.mockk.impl.recording

import io.mockk.MockKGateway
import kotlin.reflect.KClass

open class AutoHinter {
    open fun <T> autoHint(
        callRecorder: MockKGateway.CallRecorder,
        i: Int,
        n: Int,
        block: () -> T,
        blockClass: KClass<*>
    ) {
        callRecorder.round(i, n)
        block()
    }
}
