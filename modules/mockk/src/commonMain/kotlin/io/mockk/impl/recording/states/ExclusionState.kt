package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway.ExclusionParameters
import io.mockk.impl.recording.CommonCallRecorder

class ExclusionState(
    recorder: CommonCallRecorder,
    val params: ExclusionParameters
) : RecordingState(recorder) {

    override fun wasNotCalled(list: List<Any>) {
        throw MockKException("`wasNot called` is not allowed in exclude { ... } block.")
    }

    override fun recordingDone(): CallRecordingState {
        checkMissingCalls()

        for (call in recorder.calls) {
            val matcher = call.matcher
            val stub = recorder.stubRepo.stubFor(matcher.self)
            if (call.selfChain == null) {
                stub.excludeRecordedCalls(params, matcher)
            }
        }

        return recorder.factories.answeringState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException("Missing calls inside exclude { ... } block.")
        }
    }

}