package io.mockk.impl.log

import kotlin.reflect.KClass

class NativeConsoleLogger(cls: KClass<*>) : Logger {
    override fun error(msg: () -> String) = println("ERROR " + msg())
    override fun error(ex: Throwable, msg: () -> String) { println("ERROR " + msg()); ex.printStackTrace() }
    override fun warn(msg: () -> String) = println("WARN " + msg())
    override fun warn(ex: Throwable, msg: () -> String)  { println("WARN " + msg()); ex.printStackTrace() }
    override fun info(msg: () -> String) = println("INFO " + msg())
    override fun info(ex: Throwable, msg: () -> String)  { println("INFO " + msg()); ex.printStackTrace() }
    override fun debug(msg: () -> String) = println("DEBUG " + msg())
    override fun debug(ex: Throwable, msg: () -> String)  { println("DEBUG " + msg()); ex.printStackTrace() }
    override fun trace(msg: () -> String) = println("TRACE " + msg())
    override fun trace(ex: Throwable, msg: () -> String) { println("TRACE " + msg()); ex.printStackTrace() }
}