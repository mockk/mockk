package io.mockk.impl.eval

import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKMatcherScope
import io.mockk.Runs
import io.mockk.impl.InternalPlatform
import io.mockk.impl.testEvery
import io.mockk.impl.testMockk
import io.mockk.impl.recording.AutoHinter
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

open class EveryBlockEvaluatorTest {
    lateinit var evaluator: EveryBlockEvaluator
    lateinit var callRecorder: CallRecorder
    lateinit var autoHinter: AutoHinter

    @BeforeTest
    open fun setUp() {
        callRecorder = testMockk()
        autoHinter = testMockk()
        evaluator = EveryBlockEvaluator({ callRecorder }, { autoHinter })
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

        testEvery { callRecorder.estimateCallRounds() } returns estimateCallRounds
        testEvery { autoHinter.autoHint<Unit>(callRecorder, any(), any(), invoke()) } just Runs

        evaluator.every(mockBlock, null)

        assertEquals(expectLambdaCalls, counter)
    }
}