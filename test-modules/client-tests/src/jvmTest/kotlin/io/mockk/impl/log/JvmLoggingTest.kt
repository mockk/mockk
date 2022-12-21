package io.mockk.impl.log

import io.mockk.impl.log.JvmLogging.slf4jOrJulLogging
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JvmLoggingTest {

    @Test
    fun `verify slf4j logging used when slf4j is present in classpath`() {
        val logFactory = slf4jOrJulLogging()
        val logger = logFactory(Object::class)
        assertEquals(Slf4jLogger::class, logger::class)
    }

}