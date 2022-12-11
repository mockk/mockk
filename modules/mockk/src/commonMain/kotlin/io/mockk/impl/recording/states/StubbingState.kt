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
            throw MockKException("Missing mocked calls inside every { ... } block: make sure the object inside the block is a mock")
        }
    }
}
