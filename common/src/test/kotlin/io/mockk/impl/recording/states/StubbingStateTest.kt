package io.mockk.impl.recording.states

import io.mockk.Called
import io.mockk.MockKException
import io.mockk.impl.every
import io.mockk.impl.mockk
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith


class StubbingStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: StubbingState

    @BeforeTest
    fun setUp() {
        recorder = mockk()
        state = StubbingState(recorder)
    }

    @Test
    fun givenEmptyCallListInStubbingStateWhenRecordingDoneIsCalledThenExceptionIsThrown() {
        every { recorder.calls.isEmpty() } returns true

        assertFailsWith<MockKException> {
            state.recordingDone()
        }

        verify {
            recorder.factories.stubbingAwaitingAnswerCallRecorderState wasNot Called
        }
    }

    @Test
    fun givenNonEmptyCallListInStubbingStateWhenRecordingDoneIsCalledThenStateSwitchedToAnswering() {
        every { recorder.calls.isEmpty() } returns false
        every { recorder.factories.stubbingAwaitingAnswerCallRecorderState(any()) } returns mockk()

        state.recordingDone()

        verify { recorder.factories.stubbingAwaitingAnswerCallRecorderState(any()) }
    }
}
