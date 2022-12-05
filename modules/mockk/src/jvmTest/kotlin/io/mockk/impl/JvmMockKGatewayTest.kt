package io.mockk.impl

import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test

class JvmMockKGatewayTest {
    @Test
    fun whenNewGatewayInitializedNoExceptionThrown() {
        assertTrue(false)
        JvmMockKGateway()
    }
}
