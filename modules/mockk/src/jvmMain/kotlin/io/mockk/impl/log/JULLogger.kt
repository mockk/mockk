package io.mockk.impl.log

import java.util.logging.Level
import kotlin.reflect.KClass

class JULLogger(cls: KClass<*>) : Logger {
    val log: java.util.logging.Logger = java.util.logging.Logger.getLogger(cls.java.name)

    override fun error(msg: () -> String) = if (log.isLoggable(Level.SEVERE)) log.severe(msg()) else Unit
    override fun error(ex: Throwable, msg: () -> String) =
        if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, msg(), ex) else Unit

    override fun warn(msg: () -> String) = if (log.isLoggable(Level.WARNING)) log.warning(msg()) else Unit
    override fun warn(ex: Throwable, msg: () -> String) =
        if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, msg(), ex) else Unit

    override fun info(msg: () -> String) = if (log.isLoggable(Level.INFO)) log.info(msg()) else Unit
    override fun info(ex: Throwable, msg: () -> String) =
        if (log.isLoggable(Level.INFO)) log.log(Level.INFO, msg(), ex) else Unit

    override fun debug(msg: () -> String) = if (log.isLoggable(Level.FINE)) log.fine(msg()) else Unit
    override fun debug(ex: Throwable, msg: () -> String) =
        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, msg(), ex) else Unit

    override fun trace(msg: () -> String) = if (log.isLoggable(Level.FINER)) log.finer(msg()) else Unit
    override fun trace(ex: Throwable, msg: () -> String) =
        if (log.isLoggable(Level.FINER)) log.log(Level.FINER, msg(), ex) else Unit
}