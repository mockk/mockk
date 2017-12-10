package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.impl.mockk
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CallRecorderStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: CallRecorderState
    lateinit var ops: List<() -> Any?>

    @BeforeTest
    fun setUp() {
        recorder = mockk()
        state = object : CallRecorderState(recorder) {
        }
        ops = listOf(
                { state.call(mockk()) },
                { state.startStubbing() },
                { state.startVerification(mockk()) },
                { state.round(1, 1) },
                { state.answer(mockk()) },
                { state.recordingDone() },
                { state.nCalls() },
                { state.estimateCallRounds() },
                { state.wasNotCalled(mockk()) })
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