package io.mockk.impl

internal class VerifyingCallRecorderState(recorder: CallRecorderImpl) : RecordingCallRecorderState(recorder) {
    override fun doneVerification() = AnsweringCallRecorderState(recorder)
}