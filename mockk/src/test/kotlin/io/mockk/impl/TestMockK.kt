@file:Suppress("NOTHING_TO_INLINE")

package io.mockk.impl

import io.mockk.*
import io.mockk.impl.JvmMockKGateway.Companion.useImpl

actual inline fun <reified T : Any> mockk(): T = useImpl {
    MockKDsl.internalMockk(relaxed = true)
}

actual inline fun <reified T : Any> spyk(obj: T): T = useImpl {
    MockKDsl.internalSpyk(obj)
}

actual inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = useImpl {
    MockKDsl.internalEvery(stubBlock)
}

actual inline fun verify(noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerify(verifyBlock = verifyBlock)
}