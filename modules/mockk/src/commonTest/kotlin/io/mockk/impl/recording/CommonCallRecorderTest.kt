package io.mockk.impl.recording

import io.mockk.MockKGateway.VerificationParameters
import io.mockk.Ordering
import io.mockk.every
import io.mockk.impl.recording.states.CallRecordingState
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CommonCallRecorderTest {
    private lateinit var commonCallRecorder: CommonCallRecorder
    private lateinit var initState: CallRecordingState
    private lateinit var hinter: ChildHinter
    private lateinit var factories: CallRecorderFactories

    @BeforeTest
    fun setUp() {
        initState = mockk(relaxed = true)
        hinter = mockk(relaxed = true)
        factories = mockk(relaxed = true)

        val initStateFactory = mockk<(CommonCallRecorder) -> CallRecordingState>(relaxed = true)
        every { initStateFactory(any()) } returns initState

        every { factories.childHinter() } returns hinter
        every { factories.verifyingState.invoke(any(), any()) } returns mockk()
        every { factories.stubbingState.invoke(any()) } returns mockk()

        commonCallRecorder = CommonCallRecorder(
            mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true), mockk(relaxed = true), factories, initStateFactory,
            mockk(relaxed = true)
        )
    }

    @Test
    fun givenCallRecorderWhenStartStubbingThenCurrentStateStartStubbingCalled() {
        commonCallRecorder.startStubbing()
        verify { factories.stubbingState.invoke(any()) }
    }

    @Test
    fun givenCallRecorderWhenStartVerificationThenCurrentStateStartVerificationCalled() {
        val params = VerificationParameters(Ordering.ORDERED, 1, 1, false, 0)
        commonCallRecorder.startVerification(params)
        verify { factories.verifyingState.invoke(any(), any()) }
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
        commonCallRecorder.matcher<Any>(mockk(relaxed = true), mockk(relaxed = true))
        verify { initState.matcher(any(), any()) }
    }

    @Test
    fun givenCallRecorderWhenCallCalledThenCurrentStateCallCalled() {
        commonCallRecorder.call(mockk(relaxed = true))
        verify { initState.call(any()) }
    }

    @Test
    fun givenCallRecorderWhenAnswerCalledThenCurrentStateAnswerCalled() {
        commonCallRecorder.answerOpportunity()
        verify { initState.answerOpportunity() }
    }

    @Test
    fun givenCallRecorderWhenEstimateCallRoundsCalledThenCurrentStateEstimateCallRoundsCalled() {
        commonCallRecorder.estimateCallRounds()
        verify { initState.estimateCallRounds() }
    }

    @Test
    fun givenCallRecorderWhenWasNotCalledCalledThenCurrentStateWasNotCalledCalled() {
        commonCallRecorder.wasNotCalled(mockk(relaxed = true))
        verify { initState.wasNotCalled(any()) }
    }

    @Test
    fun givenCallRecorderWhenHintCalledThenCurrentStateHintCalled() {
        commonCallRecorder.hintNextReturnType(mockk(relaxed = true), 1)
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
        val safeState = mockk<CallRecordingState>(relaxed = true)
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
