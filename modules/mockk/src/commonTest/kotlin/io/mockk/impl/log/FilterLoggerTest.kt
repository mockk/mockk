package io.mockk.impl.log

import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FilterLoggerTest {
    lateinit var logger: Logger
    lateinit var traceLogger: FilterLogger
    lateinit var disabledLogger: FilterLogger
    lateinit var ex: Exception

    @BeforeTest
    fun setUp() {
        logger = mockk(relaxed = true)
        traceLogger = FilterLogger(logger) { LogLevel.TRACE }
        disabledLogger = FilterLogger(logger) { LogLevel.DISABLED }
        ex = Exception()
    }

    @Test
    fun givenTraceLevelWhenErrorMessageThenItsLogged() {
        traceLogger.error { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.error(capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenErrorMessageThenItsNotLogged() {
        disabledLogger.error { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenErrorMessageWithExceptionThenItsLogged() {
        traceLogger.error(ex) { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.error(refEq(ex), capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenErrorMessageWithExceptionThenItsNotLogged() {
        disabledLogger.error(ex) { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenWarnMessageThenItsLogged() {
        traceLogger.warn { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.warn(capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenWarnMessageThenItsNotLogged() {
        disabledLogger.warn { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenWarnMessageWithExceptionThenItsLogged() {
        traceLogger.warn(ex) { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.warn(refEq(ex), capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenWarnMessageWithExceptionThenItsNotLogged() {
        disabledLogger.warn(ex) { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenInfoMessageThenItsLogged() {
        traceLogger.info { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.info(capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenInfoMessageThenItsNotLogged() {
        disabledLogger.info { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenInfoMessageWithExceptionThenItsLogged() {
        traceLogger.info(ex) { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.info(refEq(ex), capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenInfoMessageWithExceptionThenItsNotLogged() {
        disabledLogger.info(ex) { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenDebugMessageThenItsLogged() {
        traceLogger.debug { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.debug(capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenDebugMessageThenItsNotLogged() {
        disabledLogger.debug { "msg" }

        verify {
            logger wasNot Called
        }
    }

    @Test
    fun givenTraceLevelWhenDebugMessageWithExceptionThenItsLogged() {
        traceLogger.debug(ex) { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.debug(refEq(ex), capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenDebugMessageWithExceptionThenItsNotLogged() {
        disabledLogger.debug(ex) { "msg" }

        verify {
            logger wasNot Called
        }
    }

    @Test
    fun givenTraceLevelWhenTraceMessageThenItsLogged() {
        traceLogger.trace { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.trace(capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenTraceMessageThenItsNotLogged() {
        disabledLogger.trace { "msg" }

        verify {
            logger wasNot Called
        }
    }


    @Test
    fun givenTraceLevelWhenTraceMessageWithExceptionThenItsLogged() {
        traceLogger.trace(ex) { "msg" }

        val msgLambda = CapturingSlot<() -> String>()
        verify {
            logger.trace(refEq(ex), capture(msgLambda))
        }

        assertEquals("msg", msgLambda.captured())
    }

    @Test
    fun givenDisabledLevelWhenTraceMessageWithExceptionThenItsNotLogged() {
        disabledLogger.trace(ex) { "msg" }

        verify {
            logger wasNot Called
        }
    }
}
