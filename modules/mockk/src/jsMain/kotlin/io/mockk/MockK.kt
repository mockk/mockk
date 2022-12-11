package io.mockk

import io.mockk.impl.JsMockKGateway

actual object MockK {
    actual inline fun <T> useImpl(block: () -> T): T {
        MockKGateway.implementation = JsMockKGateway.defaultImplementationBuilder
        return block()
    }
}
