package io.mockk.impl.log

class FilterLogger(val logger: Logger, val logLevel: () -> LogLevel) : Logger {

    override fun error(msg: () -> String) {
        if (logLevel() >= LogLevel.ERROR) {
            logger.error(msg)
        }
    }

    override fun error(ex: Throwable, msg: () -> String) {
        if (logLevel() >= LogLevel.ERROR) {
            logger.error(ex, msg)
        }
    }

    override fun warn(msg: () -> String) {
        if (logLevel() >= LogLevel.WARN) {
            logger.warn(msg)
        }
    }

    override fun warn(ex: Throwable, msg: () -> String) {
        if (logLevel() >= LogLevel.WARN) {
            logger.warn(ex, msg)
        }
    }

    override fun info(msg: () -> String) {
        if (logLevel() >= LogLevel.INFO) {
            logger.info(msg)
        }
    }

    override fun info(ex: Throwable, msg: () -> String) {
        if (logLevel() >= LogLevel.INFO) {
            logger.info(ex, msg)
        }
    }

    override fun debug(msg: () -> String) {
        if (logLevel() >= LogLevel.DEBUG) {
            logger.debug(msg)
        }
    }

    override fun debug(ex: Throwable, msg: () -> String) {
        if (logLevel() >= LogLevel.DEBUG) {
            logger.debug(ex, msg)
        }
    }

    override fun trace(msg: () -> String) {
        if (logLevel() >= LogLevel.TRACE) {
            logger.trace(msg)
        }
    }

    override fun trace(ex: Throwable, msg: () -> String) {
        if (logLevel() >= LogLevel.TRACE) {
            logger.trace(ex, msg)
        }
    }
}
