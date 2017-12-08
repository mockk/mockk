package io.mockk.impl.recording.states

import io.mockk.impl.recording.CallRecorderImpl

class StubbingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun recordingDone(): CallRecorderState {
        return recorder.factories.stubbingAwaitingAnswerCallRecorderState(recorder)
    }
}