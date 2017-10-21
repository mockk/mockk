package io.mockk.external

import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.util.logging.Level

internal fun Any?.toStr() =
        when (this) {
            null -> "null"
            is Method -> name + "(" + parameterTypes.map { it.simpleName }.joinToString() + ")"
            else -> toString()
        }

internal inline fun <reified T> logger(): Logger = loggerFactory(T::class.java)

internal interface Logger {
    fun error(msg: () -> String)
    fun error(ex: Throwable, msg: () -> String)
    fun warn(msg: () -> String)
    fun warn(ex: Throwable, msg: () -> String)
    fun info(msg: () -> String)
    fun info(ex: Throwable, msg: () -> String)
    fun debug(msg: () -> String)
    fun debug(ex: Throwable, msg: () -> String)
}


private val loggerFactory = try {
    Class.forName("org.slf4j.Logger");
    { cls: Class<*> -> Slf4jLogger(cls) }
} catch (ex: ClassNotFoundException) {
    { cls: Class<*> -> JULLogger(cls) }
}

private class Slf4jLogger(cls: Class<*>) : Logger {
    val log = LoggerFactory.getLogger(cls)

    override fun error(msg: () -> String) = if (log.isErrorEnabled) log.error(msg()) else Unit
    override fun error(ex: Throwable, msg: () -> String) = if (log.isErrorEnabled) log.error(msg(), ex) else Unit
    override fun warn(msg: () -> String) = if (log.isWarnEnabled) log.warn(msg()) else Unit
    override fun warn(ex: Throwable, msg: () -> String) = if (log.isWarnEnabled) log.warn(msg(), ex) else Unit
    // note library info & debug is shifted to debug & trace respectively
    override fun info(msg: () -> String) = if (log.isDebugEnabled) log.debug(msg()) else Unit

    override fun info(ex: Throwable, msg: () -> String) = if (log.isDebugEnabled) log.debug(msg(), ex) else Unit
    override fun debug(msg: () -> String) = if (log.isTraceEnabled) log.trace(msg()) else Unit
    override fun debug(ex: Throwable, msg: () -> String) = if (log.isTraceEnabled) log.trace(msg(), ex) else Unit
}

private class JULLogger(cls: Class<*>) : Logger {
    val log = java.util.logging.Logger.getLogger(cls.name)

    override fun error(msg: () -> String) = if (log.isLoggable(Level.SEVERE)) log.severe(msg()) else Unit
    override fun error(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, msg(), ex) else Unit
    override fun warn(msg: () -> String) = if (log.isLoggable(Level.WARNING)) log.warning(msg()) else Unit
    override fun warn(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, msg(), ex) else Unit
    // note library info & debug is shifted to debug & trace respectively
    override fun info(msg: () -> String) = if (log.isLoggable(Level.FINE)) log.fine(msg()) else Unit

    override fun info(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.FINE)) log.log(Level.FINE, msg(), ex) else Unit
    override fun debug(msg: () -> String) = if (log.isLoggable(Level.FINER)) log.finer(msg()) else Unit
    override fun debug(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.FINER)) log.log(Level.FINER, msg(), ex) else Unit
}
