package io.mockk.impl

internal class StubbingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun recordingDone(): CallRecorderState {
        return recorder.factories.stubbingAwaitingAnswerCallRecorderState(recorder)
    }
}