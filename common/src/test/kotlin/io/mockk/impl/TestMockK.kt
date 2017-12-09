package io.mockk.impl

import io.mockk.*

expect inline fun <reified T : Any> testMockk(): T

expect inline fun <T> testEvery(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T>

expect inline fun testVerify(noinline verifyBlock: MockKVerificationScope.() -> Unit)