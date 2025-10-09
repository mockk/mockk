package io.mockk.impl.eval

import io.mockk.*
import io.mockk.MockKGateway.CallRecorder
import io.mockk.impl.recording.AutoHinter
import kotlin.test.*

class RecordedBlockEvaluatorTest {
    lateinit var evaluator: RecordedBlockEvaluator
    lateinit var callRecorder: CallRecorder
    lateinit var autoHinter: AutoHinter
    lateinit var scope: MockKMatcherScope

    @BeforeTest
    fun setUp() {
        callRecorder = mockk(relaxed = true)
        autoHinter = mockk(relaxed = true)
        scope = MockKMatcherScope(callRecorder, CapturingSlot())
        evaluator = object : RecordedBlockEvaluator({ callRecorder }, { autoHinter }) {}
    }

    @Test
    fun givenLambdaBlockAndEstimateCallRoundsIsOneWhenEveryEvaluatorIsCalledThenLambdaIsCalledOnce() {
        testLambdaCalls(1, 1)
    }

    @Test
    fun givenLambdaBlockAndEstimateCallRoundsIsTwoWhenEveryEvaluatorIsCalledThenLambdaIsCalledTwice() {
        testLambdaCalls(2, 2)
    }

    private fun testLambdaCalls(expectLambdaCalls: Int, estimateCallRounds: Int) {
        var counter = 0
        val mockBlock: MockKMatcherScope.() -> Unit = { counter++ }

        every { callRecorder.estimateCallRounds() } returns estimateCallRounds
        every { autoHinter.autoHint<Unit>(callRecorder, any(), any(), invoke()) } just Runs

        evaluator.record(scope, mockBlock, null)

        assertEquals(expectLambdaCalls, counter)
    }

    @Test
    fun givenCoLambdaBlockAndEstimateCallRoundsIsOneWhenEveryEvaluatorIsCalledThenLambdaIsCalledOnce() {
        testCoLambdaCalls(1, 1)
    }

    @Test
    fun givenCoLambdaBlockAndEstimateCallRoundsIsTwoWhenEveryEvaluatorIsCalledThenLambdaIsCalledTwice() {
        testCoLambdaCalls(2, 2)
    }

    private fun testCoLambdaCalls(expectLambdaCalls: Int, estimateCallRounds: Int) {
        var counter = 0
        val coMockBlock: suspend MockKMatcherScope.() -> Unit = { counter++ }

        every { callRecorder.estimateCallRounds() } returns estimateCallRounds
        every { autoHinter.autoHint<Unit>(callRecorder, any(), any(), invoke()) } just Runs

        evaluator.record(scope, null, coMockBlock)

        assertEquals(expectLambdaCalls, counter)
    }

    @Test
    fun givenNoBlocksWhenEveryEvaluatorIsCalledThenExceptionIsThrown() {
        try {
            every { autoHinter.autoHint<Unit>(callRecorder, any(), any(), invoke()) } just Runs

            evaluator.record<Unit, MockKMatcherScope>(scope, null, null)
            fail("No blocks provided. Exception should be thrown")
        } catch (ex: MockKException) {

        }
    }


    @Test
    fun givenLambdaBlockWhenEveryEvaluatorIsCalledThenDoneStateIsAchieved() {
        testLambdaCalls(1, 1)

        verify {
            callRecorder.done()
        }
    }

}