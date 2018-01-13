package io.mockk

import io.mockk.impl.JsMockKGateway

@PublishedApi
internal actual object MockK {
    actual inline fun <T> useImpl(block: () -> T): T {
        MockKGateway.implementation = JsMockKGateway.defaultImplementationBuilder
        return block()
    }
}
