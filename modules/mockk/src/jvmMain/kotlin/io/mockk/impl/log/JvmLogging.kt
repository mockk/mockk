package io.mockk.impl.log

import io.mockk.proxy.MockKAgentLogger
import kotlin.reflect.KClass

object JvmLogging {
    fun slf4jOrJulLogging(): (KClass<*>) -> Logger {
        return try {
            // If we fail to create a logger, then use Java logging.
            Slf4jLogger(JvmLogging::class);
            { cls: KClass<*> -> Slf4jLogger(cls) }
        } catch (throwable: Throwable) {
            if (throwable is ClassNotFoundException || throwable is NoClassDefFoundError) { cls: KClass<*> ->
                JULLogger(cls)
            }
            else {
                throw throwable
            }
        }
    }

    fun Logger.adaptor(): MockKAgentLogger {
        return object : MockKAgentLogger {
            override fun debug(msg: String) {
                this@adaptor.debug { msg }
            }

            override fun trace(msg: String) {
                this@adaptor.trace { msg }
            }

            override fun trace(ex: Throwable, msg: String) {
                this@adaptor.trace(ex) { msg }
            }

            override fun warn(msg: String) {
                this@adaptor.warn { msg }
            }

            override fun warn(ex: Throwable, msg: String) {
                this@adaptor.warn(ex) { msg }
            }
        }
    }
}
