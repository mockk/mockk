package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CallRecordingStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: CallRecordingState
    lateinit var ops: List<() -> Any?>

    @BeforeTest
    fun setUp() {
        recorder = mockk(relaxed = true)
        state = object : CallRecordingState(recorder) {
        }
        ops = listOf(
            { state.call(mockk(relaxed = true)) },
            { state.startStubbing() },
            { state.startVerification(mockk(relaxed = true)) },
            { state.round(1, 1) },
            { state.answerOpportunity() },
            { state.recordingDone() },
            { state.nCalls() },
            { state.estimateCallRounds() },
            { state.wasNotCalled(mockk(relaxed = true)) })
    }

    @Test
    fun givenAnyOperationWhenItsCalledForRecorderStateThenExceptionIsThrown() {
        for (op in ops) {
            assertFailsWith<MockKException> {
                op()
            }
        }
    }
}