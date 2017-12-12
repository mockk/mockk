package io.mockk.impl.log

import io.mockk.impl.recording.CommonCallRecorder

class SafeLogger(val logger: Logger, val callRecorderGetter: () -> CommonCallRecorder) : Logger {

    override fun error(msg: () -> String) {
        safeLogging {
            logger.error(msg)
        }
    }

    override fun error(ex: Throwable, msg: () -> String) {
        safeLogging {
            logger.error(ex, msg)
        }
    }

    override fun warn(msg: () -> String) {
        safeLogging {
            logger.warn(msg)
        }
    }

    override fun warn(ex: Throwable, msg: () -> String) {
        safeLogging {
            logger.warn(ex, msg)
        }
    }

    override fun info(msg: () -> String) {
        safeLogging {
            logger.info(msg)
        }
    }

    override fun info(ex: Throwable, msg: () -> String) {
        safeLogging {
            logger.info(ex, msg)
        }
    }

    override fun debug(msg: () -> String) {
        safeLogging {
            logger.debug(msg)
        }
    }

    override fun debug(ex: Throwable, msg: () -> String) {
        safeLogging {
            logger.debug(ex, msg)
        }
    }

    override fun trace(msg: () -> String) {
        safeLogging {
            logger.trace(msg)
        }
    }

    override fun trace(ex: Throwable, msg: () -> String) {
        safeLogging {
            logger.trace(ex, msg)
        }
    }

    private fun safeLogging(block: () -> Unit) {
        callRecorderGetter().safeExec(block)
    }

}
