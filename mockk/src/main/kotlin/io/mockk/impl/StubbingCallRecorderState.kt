package io.mockk.impl

internal class StubbingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun done(): CallRecorderState {
        return StubbingAwaitingAnswerCallRecorderState(recorder)
    }
}