package io.mockk.impl.log

import io.mockk.impl.log.JvmLogging.slf4jOrJulLogging
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

// This test requires a separate sub module to exclude the slf4j library.
class JvmLoggingTest {

    @Test
    fun `verify Java util logging used when slf4j not present in classpath`() {
        val logFactory = slf4jOrJulLogging()
        val logger = logFactory(Object::class)
        assertEquals(JULLogger::class, logger::class)
    }
}