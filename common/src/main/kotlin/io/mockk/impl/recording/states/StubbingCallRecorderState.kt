package io.mockk.impl.recording.states

import io.mockk.impl.recording.CommonCallRecorder

class StubbingCallRecorderState(recorder: CommonCallRecorder) : RecordingCallRecorderState(recorder) {
    override fun recordingDone(): CallRecorderState {
        return recorder.factories.stubbingAwaitingAnswerCallRecorderState(recorder)
    }
}