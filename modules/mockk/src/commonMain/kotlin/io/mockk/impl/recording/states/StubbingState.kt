package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.MockKStub
import io.mockk.impl.stub.MockType

class StubbingState(
    recorder: CommonCallRecorder,
) : RecordingState(recorder) {
    override fun recordingDone(): CallRecordingState {
        checkMissingCalls()
        return recorder.factories.stubbingAwaitingAnswerState(recorder)
    }

    private fun checkMissingCalls() {
        if (recorder.calls.isEmpty()) {
            throw MockKException(
                "Missing mocked calls inside every { ... } block: make sure the object inside the block is a mock " +
                    "\nNote: if you tried to stub a Kotlin inline function, it cannot be mocked. " +
                    "Inline functions are inlined at call sites, so no call is recorded. " +
                    "Extract a non-inline wrapper or mock the dependencies used inside the inline function." +
                    enumConstEvaluationNote(),
            )
        }
    }

    /**
     * Kotlin 1.9.20's const-evaluation phase folds `MyEnum.A.name` on a statically known entry
     * into a compile-time constant, including inside recorded blocks, so no call reaches the
     * mocked entry even though `mockkObject` instrumented it correctly (issue #1198). Capturing
     * the entry in a local variable defeats the folding, because the compiler can no longer
     * prove which entry the reference resolves to.
     */
    private fun enumConstEvaluationNote(): String =
        if (recorder.stubRepo.allStubs.any { it is MockKStub && it.mockType == MockType.OBJECT && it.type.java.isEnum }) {
            "\nNote: if you tried to stub `name` of a Kotlin enum entry referenced statically " +
                "(for example every { MyEnum.A.name }), the Kotlin compiler since 1.9.20 evaluates it to a " +
                "constant at compile time, so no call is recorded on the mocked entry. " +
                "Capture the entry in a local variable first (val entry = MyEnum.A) and use that variable " +
                "both inside the block and later on, to read the stubbed value."
        } else {
            ""
        }
}
