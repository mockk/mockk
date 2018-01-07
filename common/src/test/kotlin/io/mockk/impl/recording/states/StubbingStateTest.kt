package io.mockk.impl.recording.states

import io.mockk.*
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith


class StubbingStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: StubbingState

    @BeforeTest
    fun setUp() {
        recorder = mockk(relaxed = true)
        state = StubbingState(recorder)
    }

    @Test
    fun givenEmptyCallListInStubbingStateWhenRecordingDoneIsCalledThenExceptionIsThrown() {
        every { recorder.calls.isEmpty() } returns true

        assertFailsWith<MockKException> {
            state.recordingDone()
        }

        verify {
            recorder.factories.stubbingAwaitingAnswerState wasNot Called
        }
    }

    @Test
    fun givenNonEmptyCallListInStubbingStateWhenRecordingDoneIsCalledThenStateSwitchedToAnswering() {
        every { recorder.calls.isEmpty() } returns false
        every { recorder.factories.stubbingAwaitingAnswerState(any()) } returns mockk(relaxed = true)

        state.recordingDone()

        verify { recorder.factories.stubbingAwaitingAnswerState(any()) }
    }
}
