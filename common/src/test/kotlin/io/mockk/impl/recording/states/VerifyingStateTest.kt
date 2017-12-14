package io.mockk.impl.recording.states

import io.mockk.MockKException
import io.mockk.MockKGateway
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.impl.every
import io.mockk.impl.mockk
import io.mockk.impl.recording.CommonCallRecorder
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class VerifyingStateTest {
    lateinit var state: VerifyingState
    lateinit var recorder: CommonCallRecorder

    @BeforeTest
    fun setUp() {
        recorder = mockk()
        val params = VerificationParameters(Ordering.UNORDERED, 1, 2, false)
        state = VerifyingState(recorder, params)
    }

    @Test
    fun givenEmptyCallListInVerifyingStateWhenRecordingDoneIsCalledThenExceptionIsThrown() {
        every { recorder.calls.isEmpty() } returns true

        assertFailsWith<MockKException> {
            state.recordingDone()
        }
    }
}