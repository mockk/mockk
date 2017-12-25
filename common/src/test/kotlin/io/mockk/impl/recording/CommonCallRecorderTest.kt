package io.mockk.impl.recording

import io.mockk.MockKGateway
import io.mockk.MockKGateway.*
import io.mockk.Ordering
import io.mockk.impl.every
import io.mockk.impl.mockk
import io.mockk.impl.recording.states.CallRecordingState
import io.mockk.impl.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CommonCallRecorderTest {
    lateinit var commonCallRecorder: CommonCallRecorder
    lateinit var initState: CallRecordingState
    lateinit var hinter: ChildHinter

    @BeforeTest
    fun setUp() {
        initState = mockk()
        hinter = mockk()

        val initStateFactory = mockk<(CommonCallRecorder) -> CallRecordingState>()
        every { initStateFactory(any()) } returns initState

        val factories = mockk<CallRecorderFactories>()

        every { factories.childHinter() } returns hinter

        commonCallRecorder = CommonCallRecorder(
                mockk(), mockk(), mockk(), mockk(),
                mockk(), mockk(), factories, initStateFactory)
    }

    @Test
    fun givenCallRecorderWhenStartStubbingThenCurrentStateStartStubbingCalled() {
        commonCallRecorder.startStubbing()
        verify { initState.startStubbing() }
    }

    @Test
    fun givenCallRecorderWhenStartVerificationThenCurrentStateStartVerificationCalled() {
        val params = VerificationParameters(Ordering.ORDERED, 1, 1, false)
        commonCallRecorder.startVerification(params)
        verify { initState.startVerification(params) }
    }

    @Test
    fun givenCallRecorderWhenDoneRecordingThenCurrentStateDoneRecordingCalled() {
        commonCallRecorder.done()
        verify { initState.recordingDone() }
    }

    @Test
    fun givenCallRecorderWhenRoundThenCurrentStateDoneRecordingCalled() {
        commonCallRecorder.round(1, 1)
        verify { initState.round(1, 1) }
    }

    @Test
    fun givenCallRecorderWhenCallCountQueriedCalledThenCurrentStateCallCountIsQueried() {
        commonCallRecorder.nCalls()
        verify { initState.nCalls() }
    }

    @Test
    fun givenCallRecorderWhenMatcherCalledThenCurrentStateMatcherCalled() {
        commonCallRecorder.matcher<Any>(mockk(), mockk())
        verify { initState.matcher(any(), any()) }
    }

    @Test
    fun givenCallRecorderWhenCallCalledThenCurrentStateCallCalled() {
        commonCallRecorder.call(mockk())
        verify { initState.call(any()) }
    }

    @Test
    fun givenCallRecorderWhenAnswerCalledThenCurrentStateAnswerCalled() {
        commonCallRecorder.answer(mockk())
        verify { initState.answer(any()) }
    }

    @Test
    fun givenCallRecorderWhenEstimateCallRoundsCalledThenCurrentStateEstimateCallRoundsCalled() {
        commonCallRecorder.estimateCallRounds()
        verify { initState.estimateCallRounds() }
    }

    @Test
    fun givenCallRecorderWhenWasNotCalledCalledThenCurrentStateWasNotCalledCalled() {
        commonCallRecorder.wasNotCalled(mockk())
        verify { initState.wasNotCalled(any()) }
    }

    @Test
    fun givenCallRecorderWhenHintCalledThenCurrentStateHintCalled() {
        commonCallRecorder.hintNextReturnType(mockk(), 1)
        verify { hinter.hint(1, any()) }
    }

    @Test
    fun givenCallRecorderWhenDiscardLastCallRoundThenCurrentStateDiscardLastCallRoundCalled() {
        commonCallRecorder.discardLastCallRound()
        verify { initState.discardLastCallRound() }
    }

    @Test
    fun givenCallRecorderResetCalledThenStateIsReset() {
        commonCallRecorder.reset()
        verify { commonCallRecorder.factories.childHinter() }
        verify { commonCallRecorder.initialState(commonCallRecorder) }
    }

    @Test
    fun givenCallRecorderSafeExecCalledThenBlockIsEvaluatedInSafeState() {
        val safeState = mockk<CallRecordingState>()
        every { commonCallRecorder.factories.safeLoggingState(any()) } returns safeState
        var blockCalled = false
        commonCallRecorder.safeExec {
            assertSame(safeState, commonCallRecorder.state)
            blockCalled = true
        }
        verify { commonCallRecorder.factories.safeLoggingState(any()) }
        assertTrue(blockCalled)
    }

}