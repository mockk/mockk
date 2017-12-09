package io.mockk.impl.eval

import org.junit.Before
import org.junit.Test

class JvmEveryBlockEvaluatorTest : EveryBlockEvaluatorTest() {
    @Before override fun setUp() { super.setUp() }
    @Test override fun givenLambdaBlockAndEstimateCallRoundsIsOneWhenEveryEvaluatorIsCalledThenLambdaIsCalledOnce() { super.givenLambdaBlockAndEstimateCallRoundsIsOneWhenEveryEvaluatorIsCalledThenLambdaIsCalledOnce() }
    @Test override fun givenLambdaBlockAndEstimateCallRoundsIsTwoWhenEveryEvaluatorIsCalledThenLambdaIsCalledTwice() { super.givenLambdaBlockAndEstimateCallRoundsIsTwoWhenEveryEvaluatorIsCalledThenLambdaIsCalledTwice() }
}