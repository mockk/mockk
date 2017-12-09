package io.mockk.impl

import io.mockk.*
import io.mockk.impl.JvmMockKGateway.Companion.useImpl

actual inline fun <reified T : Any> testMockk(): T = useImpl {
    MockKDsl.internalMockk(relaxed = true)
}

actual inline fun <T> testEvery(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = useImpl {
    MockKDsl.internalEvery(stubBlock)
}

actual inline fun testVerify(noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerify(verifyBlock = verifyBlock)
}