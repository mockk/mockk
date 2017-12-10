package io.mockk.impl.eval

import io.mockk.CapturingSlot
import io.mockk.MockKException
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKMatcherScope
import io.mockk.Runs
import io.mockk.impl.recording.AutoHinter
import io.mockk.impl.every
import io.mockk.impl.mockk
import io.mockk.impl.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

open class RecordedBlockEvaluatorTest {
    lateinit var evaluator: RecordedBlockEvaluator
    lateinit var callRecorder: CallRecorder
    lateinit var autoHinter: AutoHinter
    lateinit var scope: MockKMatcherScope

    @BeforeTest
    open fun setUp() {
        callRecorder = mockk()
        autoHinter = mockk()
        scope = MockKMatcherScope(callRecorder, CapturingSlot())
        evaluator = object : RecordedBlockEvaluator({ callRecorder }, { autoHinter }) {}
    }

    @Test
    open fun givenLambdaBlockAndEstimateCallRoundsIsOneWhenEveryEvaluatorIsCalledThenLambdaIsCalledOnce() {
        testLambdaCalls(1, 1)
    }

    @Test
    open fun givenLambdaBlockAndEstimateCallRoundsIsTwoWhenEveryEvaluatorIsCalledThenLambdaIsCalledTwice() {
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
    open fun givenCoLambdaBlockAndEstimateCallRoundsIsOneWhenEveryEvaluatorIsCalledThenLambdaIsCalledOnce() {
        testCoLambdaCalls(1, 1)
    }

    @Test
    open fun givenCoLambdaBlockAndEstimateCallRoundsIsTwoWhenEveryEvaluatorIsCalledThenLambdaIsCalledTwice() {
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
    open fun givenNoBlocksWhenEveryEvaluatorIsCalledThenExceptionIsThrown() {
        try {
            every { autoHinter.autoHint<Unit>(callRecorder, any(), any(), invoke()) } just Runs

            evaluator.record<Unit, MockKMatcherScope>(scope,null, null)
            fail("No blocks provided. Exception should be thrown")
        } catch (ex: MockKException) {

        }
    }


    @Test
    open fun givenLambdaBlockWhenEveryEvaluatorIsCalledThenDoneStateIsAchieved() {
        testLambdaCalls(1, 1)

        verify {
            callRecorder.done()
        }
    }

}