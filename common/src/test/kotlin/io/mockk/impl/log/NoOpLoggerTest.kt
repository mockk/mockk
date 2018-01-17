package io.mockk.impl.log

import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class NoOpLoggerTest {
    lateinit var logger: NoOpLogger
    lateinit var msgLambda: () -> String
    lateinit var ex: Exception

    @BeforeTest
    fun setUp() {
        logger = NoOpLogger()
        msgLambda = mockk(relaxed = true)
        ex = mockk(relaxed = true)
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToErrorNoOpLoggerThenItsNotEvaluated() {
        logger.error(msgLambda)

        verify {
            msgLambda wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToErrorWithExceptionNoOpLoggerThenItsNotEvaluated() {
        logger.error(ex, msgLambda)

        verify {
            listOf(ex, msgLambda) wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToWarnNoOpLoggerThenItsNotEvaluated() {
        logger.warn(msgLambda)

        verify {
            msgLambda wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToWarnWithExceptionNoOpLoggerThenItsNotEvaluated() {
        logger.warn(ex, msgLambda)

        verify {
            listOf(ex, msgLambda) wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToInfoNoOpLoggerThenItsNotEvaluated() {
        logger.info(msgLambda)

        verify {
            msgLambda wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToInfoWithExceptionNoOpLoggerThenItsNotEvaluated() {
        logger.info(ex, msgLambda)

        verify {
            listOf(ex, msgLambda) wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToDebugNoOpLoggerThenItsNotEvaluated() {
        logger.debug(msgLambda)

        verify {
            msgLambda wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToDebugWithExceptionNoOpLoggerThenItsNotEvaluated() {
        logger.debug(ex, msgLambda)

        verify {
            listOf(ex, msgLambda) wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToTraceNoOpLoggerThenItsNotEvaluated() {
        logger.trace(msgLambda)

        verify {
            msgLambda wasNot Called
        }
    }

    @Test
    fun givenMessageLambdaWhenItIsPassedToTraceWithExceptionNoOpLoggerThenItsNotEvaluated() {
        logger.trace(ex, msgLambda)

        verify {
            listOf(ex, msgLambda) wasNot Called
        }
    }
}