package io.mockk.impl.recording

import io.mockk.MockKGateway.CallRecorder
import io.mockk.impl.mockk
import io.mockk.impl.verify

class AutoHinterTest {
    val recorder = mockk<CallRecorder>()
    val autoHinter = AutoHinter()
    val block = mockk<() -> Unit>()

    internal fun givenRecorderWhenAutoHinterIsCalledShouldDeclareNextRoundAndCallBlock() {
        autoHinter.autoHint(recorder, 0, 0, block)

        verify { recorder.round(0, 0) }
        verify { block.invoke() }
    }
}