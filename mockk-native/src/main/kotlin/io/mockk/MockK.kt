package io.mockk

import io.mockk.impl.NativeMockKGateway

actual object MockK {
    actual inline fun <T> useImpl(block: () -> T): T {
        MockKGateway.implementation = NativeMockKGateway.defaultImplementationBuilder
        return block()
    }
}
