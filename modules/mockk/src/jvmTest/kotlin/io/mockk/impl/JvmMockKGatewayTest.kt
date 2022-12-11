package io.mockk.impl

import kotlin.test.Test

class JvmMockKGatewayTest {
    @Test
    fun whenNewGatewayInitializedNoExceptionThrown() {
        JvmMockKGateway()
    }
}