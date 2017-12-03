package io.mockk.impl

import kotlin.reflect.KClass

internal interface Logger {
    fun error(msg: () -> String)
    fun error(ex: Throwable, msg: () -> String)
    fun warn(msg: () -> String)
    fun warn(ex: Throwable, msg: () -> String)
    fun info(msg: () -> String)
    fun info(ex: Throwable, msg: () -> String)
    fun debug(msg: () -> String)
    fun debug(ex: Throwable, msg: () -> String)
    fun trace(msg: () -> String)
    fun trace(ex: Throwable, msg: () -> String)

    companion object {
        internal var loggerFactory : (KClass<*>) -> Logger = { NoOpLogger() }
        internal inline operator fun <reified T> invoke() = loggerFactory(T::class)
    }
}
