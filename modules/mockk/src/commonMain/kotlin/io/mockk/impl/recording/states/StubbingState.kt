package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.impl.recording.CommonCallRecorder

class StubbingState(recorder: CommonCallRecorder) : RecordingState(recorder) {
    override fun recordingDone(): CallRecordingState {
        checkMissingCalls()
        return recorder.factories.stubbingAwaitingAnswerState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException(
                "Missing mocked calls inside every { ... } block: make sure the object inside the block is a mock " +
                        "\nNote: if you tried to stub a Kotlin inline function, it cannot be mocked. " +
                        "Inline functions are inlined at call sites, so no call is recorded. " +
                        "Extract a non-inline wrapper or mock the dependencies used inside the inline function."

            )
        }
    }
}
