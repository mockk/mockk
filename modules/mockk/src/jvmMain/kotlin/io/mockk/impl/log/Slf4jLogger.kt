package io.mockk.impl.log

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class Slf4jLogger(cls: KClass<*>) : Logger {
    val log: org.slf4j.Logger = LoggerFactory.getLogger(cls.java)

    override fun error(msg: () -> String) = if (log.isErrorEnabled) log.error(msg()) else Unit
    override fun error(ex: Throwable, msg: () -> String) = if (log.isErrorEnabled) log.error(msg(), ex) else Unit
    override fun warn(msg: () -> String) = if (log.isWarnEnabled) log.warn(msg()) else Unit
    override fun warn(ex: Throwable, msg: () -> String) = if (log.isWarnEnabled) log.warn(msg(), ex) else Unit
    override fun info(msg: () -> String) = if (log.isInfoEnabled) log.info(msg()) else Unit
    override fun info(ex: Throwable, msg: () -> String) = if (log.isInfoEnabled) log.info(msg(), ex) else Unit
    override fun debug(msg: () -> String) = if (log.isDebugEnabled) log.debug(msg()) else Unit
    override fun debug(ex: Throwable, msg: () -> String) = if (log.isDebugEnabled) log.debug(msg(), ex) else Unit
    override fun trace(msg: () -> String) = if (log.isTraceEnabled) log.trace(msg()) else Unit
    override fun trace(ex: Throwable, msg: () -> String) = if (log.isTraceEnabled) log.trace(msg(), ex) else Unit
}