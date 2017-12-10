package io.mockk.impl

import kotlin.test.*

class JvmMockKGatewayTest {
    @Test
    fun whenNewGatewayInitializedNoExceptionThrown() {
        JvmMockKGateway()
    }
}