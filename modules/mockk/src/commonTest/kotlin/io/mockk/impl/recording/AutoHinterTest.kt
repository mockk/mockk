package io.mockk.impl.recording

import io.mockk.MockKGateway.CallRecorder
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class AutoHinterTest {
    val recorder = mockk<CallRecorder>(relaxed = true)
    val autoHinter = AutoHinter()
    val block = mockk<() -> Unit>(relaxed = true)

    @Test
    internal fun givenRecorderWhenAutoHinterIsCalledShouldDeclareNextRoundAndCallBlock() {
        autoHinter.autoHint(recorder, 0, 0, block)

        verify { recorder.round(0, 0) }
        verify { block.invoke() }
    }
}
