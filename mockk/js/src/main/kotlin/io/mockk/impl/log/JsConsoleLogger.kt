package io.mockk.impl.log

import kotlin.reflect.KClass

class JsConsoleLogger(cls: KClass<*>) : Logger {
    override fun error(msg: () -> String) = console.error(msg())
    override fun error(ex: Throwable, msg: () -> String) = console.error(msg(), js("ex.stack"))
    override fun warn(msg: () -> String) = console.warn(msg())
    override fun warn(ex: Throwable, msg: () -> String) = console.warn(msg(), js("ex.stack"))
    override fun info(msg: () -> String) = console.info(msg())
    override fun info(ex: Throwable, msg: () -> String) = console.info(msg(), js("ex.stack"))
    override fun debug(msg: () -> String) = console.log(msg())
    override fun debug(ex: Throwable, msg: () -> String) = console.log(msg(), js("ex.stack"))
    override fun trace(msg: () -> String) {
        val m = msg()
        js("console.debug(m)")
    }

    override fun trace(ex: Throwable, msg: () -> String) {
        val m = msg()
        js("console.debug(m, ex.stack)")
    }
}