package io.mockk

import io.mockk.impl.JvmMockKGateway
import io.mockk.proxy.safeScope

actual object MockK {
    actual inline fun <T> useImpl(block: () -> T): T {
        MockKGateway.implementation = JvmMockKGateway.defaultImplementationBuilder
        return JvmMockKGateway.defaultImplementation.interceptionScope.safeScope(true, block)
    }
}
