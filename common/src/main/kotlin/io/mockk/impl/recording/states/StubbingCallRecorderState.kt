package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.impl.recording.CommonCallRecorder

class StubbingCallRecorderState(recorder: CommonCallRecorder) : RecordingCallRecorderState(recorder) {
    override fun recordingDone(): CallRecorderState {
        checkMissingCalls()
        return recorder.factories.stubbingAwaitingAnswerCallRecorderState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside every { ... } block.")
        }
    }
}