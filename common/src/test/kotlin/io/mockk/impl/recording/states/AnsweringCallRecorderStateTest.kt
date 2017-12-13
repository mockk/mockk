package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.Stub
import io.mockk.impl.every
import io.mockk.impl.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AnsweringCallRecorderStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: AnsweringState
    lateinit var invocation: Invocation
    lateinit var stub: Stub
    lateinit var otherState: CallRecordingState

    @BeforeTest
    fun setUp() {
        recorder = mockk()
        invocation = mockk()
        state = AnsweringState(recorder)
        stub = mockk()
        otherState = mockk()
    }

    @Test
    fun givenAnsweringStateWhenCallIsCalledThenAnswerFromStubReturned() {
        every {
            recorder.stubRepo.stubFor(any())
        } returns stub

        every {
            stub.answer(invocation)
        } returns 5

        val ret = state.call(invocation)

        assertEquals(5, ret)
    }

    @Test
    fun givenAnsweringStateWhenStartStubbingIsCalledThenSwitchedToStubbingState() {
        every {
            recorder.factories.stubbingCallRecorderState(any())
        } returns otherState

        val ret = state.startStubbing()

        assertSame(otherState, ret)
    }

    fun givenAnsweringStateWhenStartVerificationIsCalledThenSwitchedToVerificationState() {
        val params = VerificationParameters(Ordering.ALL, 1, 1, false);

        every {
            recorder.factories.verifyingCallRecorderState(any(), params)
        } returns otherState

        val ret = state.startVerification(params)

        assertSame(otherState, ret)
    }
}