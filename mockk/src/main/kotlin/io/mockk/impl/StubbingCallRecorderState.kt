package io.mockk.impl

internal class StubbingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun catchArgs(round: Int, n: Int) {
        super.catchArgs(round, n)
        if (round == n) {
            recorder.state = StubbingAwaitingAnswerCallRecorderState(recorder)
        }
    }
}