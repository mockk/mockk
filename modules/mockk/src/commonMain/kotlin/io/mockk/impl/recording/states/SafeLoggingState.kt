package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.Stub

class SafeLoggingState(recorder: CommonCallRecorder) : CallRecordingState(recorder) {
    override fun call(invocation: Invocation): Any? = (invocation.stub as Stub).stdObjectAnswer(invocation)
}
