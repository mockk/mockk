package io.mockk

import io.mockk.impl.JvmMockKGateway

@PublishedApi
internal actual object MockK {
    actual inline fun <T> useImpl(block: () -> T): T {
        MockKGateway.implementation = JvmMockKGateway.defaultImplementationBuilder
        return block()
    }
}
