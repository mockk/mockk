package io.mockk.impl

import io.mockk.MockKDsl
import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.MockKVerificationScope
import io.mockk.impl.JsMockKGateway.Companion.useImpl

actual inline fun <reified T : Any> testMockk(): T = useImpl {
    MockKDsl.internalMockk(relaxed = true)
}

actual inline fun <reified T : Any> testSpyk(obj: T): T = useImpl {
    MockKDsl.internalSpyk(obj)
}

actual inline fun <T> testEvery(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = useImpl {
    MockKDsl.internalEvery(stubBlock)
}

actual inline fun testVerify(noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerify(verifyBlock = verifyBlock)
}