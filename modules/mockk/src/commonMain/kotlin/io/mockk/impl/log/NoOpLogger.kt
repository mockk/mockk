package io.mockk.impl.log

class NoOpLogger : Logger {
    override fun error(msg: () -> String) {}
    override fun error(ex: Throwable, msg: () -> String) {}
    override fun warn(msg: () -> String) {}
    override fun warn(ex: Throwable, msg: () -> String) {}
    override fun info(msg: () -> String) {}
    override fun info(ex: Throwable, msg: () -> String) {}
    override fun debug(msg: () -> String) {}
    override fun debug(ex: Throwable, msg: () -> String) {}
    override fun trace(msg: () -> String) {}
    override fun trace(ex: Throwable, msg: () -> String) {}
}
