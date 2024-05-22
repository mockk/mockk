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
        every { recorder.calls } returns mutableListOf()

        assertFailsWith<MockKException> {
            state.recordingDone()
        }

        verify(exactly = 0) { recorder.factories.stubbingAwaitingAnswerState(any()) }
    }

    @Test
    fun givenNonEmptyCallListInStubbingStateWhenRecordingDoneIsCalledThenStateSwitchedToAnswering() {
        every { recorder.calls } returns mutableListOf(mockk())
        every { recorder.factories.stubbingAwaitingAnswerState(any()) } returns mockk(relaxed = true)

        state.recordingDone()

        verify { recorder.factories.stubbingAwaitingAnswerState(any()) }
    }
}
