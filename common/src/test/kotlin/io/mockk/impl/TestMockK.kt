package io.mockk.impl

import io.mockk.*

expect inline fun <reified T : Any> mockk(): T

expect inline fun <reified T : Any> spyk(obj: T): T

expect inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T>

expect inline fun verify(noinline verifyBlock: MockKVerificationScope.() -> Unit)