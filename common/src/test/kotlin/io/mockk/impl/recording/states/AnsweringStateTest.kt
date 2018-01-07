package io.mockk.impl.recording.states

import io.mockk.Invocation
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.every
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.stub.Stub
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AnsweringStateTest {
    lateinit var recorder: CommonCallRecorder
    lateinit var state: AnsweringState
    lateinit var invocation: Invocation
    lateinit var stub: Stub
    lateinit var otherState: CallRecordingState

    @BeforeTest
    fun setUp() {
        recorder = mockk(relaxed = true)
        invocation = mockk(relaxed = true)
        state = AnsweringState(recorder)
        stub = mockk(relaxed = true)
        otherState = mockk(relaxed = true)
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
            recorder.factories.stubbingState(any())
        } returns otherState

        val ret = state.startStubbing()

        assertSame(otherState, ret)
    }

    fun givenAnsweringStateWhenStartVerificationIsCalledThenSwitchedToVerificationState() {
        val params = VerificationParameters(Ordering.ALL, 1, 1, false);

        every {
            recorder.factories.verifyingState(any(), params)
        } returns otherState

        val ret = state.startVerification(params)

        assertSame(otherState, ret)
    }
}