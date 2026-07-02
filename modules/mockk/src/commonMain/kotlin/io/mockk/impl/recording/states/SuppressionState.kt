package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.impl.recording.CommonCallRecorder

class SuppressionState(
    recorder: CommonCallRecorder,
) : RecordingState(recorder) {
    override fun wasNotCalled(list: List<Any>): Unit = throw MockKException("`wasNot called` is not allowed in suppress { ... } block.")

    override fun recordingDone(): CallRecordingState {
        checkMissingCalls()

        for (call in recorder.calls) {
            val matcher = call.matcher
            val stub = recorder.stubRepo.stubFor(matcher.self)
            if (call.selfChain == null) {
                stub.suppressRecordedCalls(matcher)
            }
        }

        return recorder.factories.answeringState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside suppress { ... } block.")
        }
    }
}
