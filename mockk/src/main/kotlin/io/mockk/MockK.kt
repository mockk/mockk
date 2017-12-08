@file:Suppress("NOTHING_TO_INLINE")

package io.mockk

import io.mockk.impl.JvmMockKGateway.Companion.useImpl
import kotlin.reflect.KClass

/**
 * Builds a new mock for specified class
 */
inline fun <reified T : Any> mockk(name: String? = null, vararg moreInterfaces: KClass<*>): T = useImpl {
    MockKDsl.internalMockk(name, *moreInterfaces)
}

/**
 * Builds a new spy for specified class. Initializes object via default constructor.
 */
inline fun <reified T : Any> spyk(name: String? = null, vararg moreInterfaces: KClass<*>): T = useImpl {
    MockKDsl.internalSpyk(name, *moreInterfaces)
}

/**
 * Builds a new spy for specified class. Copies fields from provided object
 */
inline fun <reified T : Any> spyk(objToCopy: T, name: String? = null, vararg moreInterfaces: KClass<*>): T = useImpl {
    MockKDsl.internalSpyk(objToCopy, name, *moreInterfaces)
}

/**
 * Creates new capturing slot
 */
inline fun <reified T : Any> slot() = useImpl {
    MockKDsl.internalSlot<T>()
}

/**
 * Starts a block of stubbing. Part of DSL.
 */
inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = useImpl {
    MockKDsl.internalEvery(stubBlock)
}

/**
 * Starts a block of stubbing for coroutines. Part of DSL.
 */
inline fun <T> coEvery(noinline stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T> = useImpl {
    MockKDsl.internalCoEvery(stubBlock)
}

/**
 * Verifies calls happened in the past. Part of DSL
 */
inline fun verify(ordering: Ordering = Ordering.UNORDERED,
                  inverse: Boolean = false,
                  atLeast: Int = 1,
                  atMost: Int = Int.MAX_VALUE,
                  exactly: Int = -1,
                  noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerify(ordering, inverse, atLeast, atMost, exactly, verifyBlock)
}

/**
 * Verify for coroutines
 */
inline fun coVerify(ordering: Ordering = Ordering.UNORDERED,
                    inverse: Boolean = false,
                    atLeast: Int = 1,
                    atMost: Int = Int.MAX_VALUE,
                    exactly: Int = -1,
                    noinline verifyBlock: suspend MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalCoVerify(
            ordering,
            inverse,
            atLeast,
            atMost,
            exactly,
            verifyBlock)
}

/**
 * Shortcut for ordered calls verification
 */
inline fun verifyAll(inverse: Boolean = false,
                     noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerifyAll(inverse, verifyBlock)
}

/**
 * Shortcut for ordered calls verification
 */
inline fun verifyOrder(inverse: Boolean = false,
                       noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerifyOrder(inverse, verifyBlock)
}

/**
 * Shortcut for sequence calls verification
 */
inline fun verifySequence(inverse: Boolean = false,
                          noinline verifyBlock: MockKVerificationScope.() -> Unit) = useImpl {
    MockKDsl.internalVerifySequence(inverse, verifyBlock)
}

/**
 * Resets information associated with mock
 */
fun clearMocks(vararg mocks: Any, answers: Boolean = true, recordedCalls: Boolean = true, childMocks: Boolean = true) = useImpl {
    MockKDsl.internalClearMocks(mocks = *mocks,
            answers = answers,
            recordedCalls = recordedCalls,
            childMocks = childMocks)
}

/**
 * Registers instance factory and returns object able to do deregistration.
 */
inline fun <reified T : Any> registerInstanceFactory(noinline instanceFactory: () -> T): Deregisterable = useImpl {
    MockKDsl.internalRegisterInstanceFactory(instanceFactory)
}


/**
 * Executes block of code with registering and unregistering instance factory.
 */
inline fun <reified T : Any, R> withInstanceFactory(noinline instanceFactory: () -> T, block: () -> R): R = useImpl {
    MockKDsl.internalWithInstanceFactory(instanceFactory, block)
}

/**
 * Builds a static mock via static mock scope.
 * To actually use it you need to call use or mock/unmock.
 */
inline fun <reified T : Any> staticMockk(): MockKStaticScope = useImpl {
    MockKDsl.internalStaticMockk<T>()
}

/**
 * Builds a static mock via static mock scope.
 * To actually use it you need to call use or mock/unmock.
 */
inline fun staticMockk(vararg cls: String): MockKStaticScope = useImpl {
    MockKDsl.internalStaticMockk(*cls.map { Class.forName(it).kotlin }.toTypedArray())
}
