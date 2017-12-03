package io.mockk.impl

import io.mockk.agent.MockKAgentLogger
import kotlin.reflect.KClass

object JvmLogging {
    internal fun slf4jOrJulLogging(): (KClass<*>) -> Logger {
        return try {
            Class.forName("org.slf4j.Logger");
            { cls: KClass<*> -> Slf4jLogger(cls) }
        } catch (ex: ClassNotFoundException) {
            { cls: KClass<*> -> JULLogger(cls) }
        }
    }

    internal fun Logger.adaptor(): MockKAgentLogger {
        return object : MockKAgentLogger {
            override fun debug(msg: String) {  this@adaptor.debug { msg } }

            override fun trace(msg: String) {  this@adaptor.trace { msg } }

            override fun trace(ex: Throwable, msg: String) {  this@adaptor.trace(ex) { msg } }

        }
    }
}