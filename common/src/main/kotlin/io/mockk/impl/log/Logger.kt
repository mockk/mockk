package io.mockk.impl.log

import kotlin.reflect.KClass

interface Logger {
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
        var loggerFactory: (KClass<*>) -> Logger = { NoOpLogger() }
        inline operator fun <reified T> invoke() = loggerFactory(T::class)
    }
}

