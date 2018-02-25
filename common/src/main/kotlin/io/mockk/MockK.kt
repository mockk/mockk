@file:Suppress("NOTHING_TO_INLINE")

package io.mockk

import kotlin.reflect.KClass

/**
 * Builds a new mock for specified class
 */
inline fun <reified T : Any> mockk(
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalMockk(name, relaxed, *moreInterfaces, block = block)
}

/**
 * Builds a new spy for specified class. Initializes object via default constructor.
 */
inline fun <reified T : Any> spyk(
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    recordPrivateCalls: Boolean = false,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalSpyk(
        name,
        *moreInterfaces,
        recordPrivateCalls = recordPrivateCalls,
        block = block
    )
}

/**
 * Builds a new spy for specified class. Copies fields from provided object
 */
inline fun <reified T : Any> spyk(
    objToCopy: T,
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    recordPrivateCalls: Boolean = false,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalSpyk(
        objToCopy,
        name,
        *moreInterfaces,
        recordPrivateCalls = recordPrivateCalls,
        block = block
    )
}

/**
 * Creates new capturing slot
 */
inline fun <reified T : Any> slot() = MockK.useImpl {
    MockKDsl.internalSlot<T>()
}

/**
 * Starts a block of stubbing. Part of DSL.
 */
inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = MockK.useImpl {
    MockKDsl.internalEvery(stubBlock)
}

/**
 * Starts a block of stubbing for coroutines. Part of DSL.
 */
inline fun <T> coEvery(noinline stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T> = MockK.useImpl {
    MockKDsl.internalCoEvery(stubBlock)
}

/**
 * Verifies calls happened in the past. Part of DSL
 */
inline fun verify(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    noinline verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerify(ordering, inverse, atLeast, atMost, exactly, verifyBlock)
}

/**
 * Verify for coroutines
 */
inline fun coVerify(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    noinline verifyBlock: suspend MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalCoVerify(
        ordering,
        inverse,
        atLeast,
        atMost,
        exactly,
        verifyBlock
    )
}

/**
 * Shortcut for ordered calls verification
 */
inline fun verifyAll(
    inverse: Boolean = false,
    noinline verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyAll(inverse, verifyBlock)
}

/**
 * Shortcut for ordered calls verification
 */
inline fun verifyOrder(
    inverse: Boolean = false,
    noinline verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyOrder(inverse, verifyBlock)
}

/**
 * Shortcut for sequence calls verification
 */
inline fun verifySequence(
    inverse: Boolean = false,
    noinline verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifySequence(inverse, verifyBlock)
}

/**
 * Resets information associated with mock
 */
fun clearMocks(vararg mocks: Any, answers: Boolean = true, recordedCalls: Boolean = true, childMocks: Boolean = true) =
    MockK.useImpl {
        MockKDsl.internalClearMocks(
            mocks = *mocks,
            answers = answers,
            recordedCalls = recordedCalls,
            childMocks = childMocks
        )
    }

/**
 * Registers instance factory and returns object able to do deregistration.
 */
inline fun <reified T : Any> registerInstanceFactory(noinline instanceFactory: () -> T): Deregisterable =
    MockK.useImpl {
        MockKDsl.internalRegisterInstanceFactory(instanceFactory)
    }


/**
 * Executes block of code with registering and unregistering instance factory.
 */
inline fun <reified T : Any, R> withInstanceFactory(noinline instanceFactory: () -> T, block: () -> R): R =
    MockK.useImpl {
        MockKDsl.internalWithInstanceFactory(instanceFactory, block)
    }

/**
 * Builds a static mock via static mock scope.
 * To actually use it you need to call use or mock/unmock/use.
 */
inline fun <reified T : Any> staticMockk(): MockKStaticScope = MockK.useImpl {
    MockKDsl.internalStaticMockk<T>()
}

/**
 * Builds a static mock via static mock scope.
 * To actually use it you need to call use or mock/unmock/use.
 */
inline fun staticMockk(vararg cls: String): MockKStaticScope = MockK.useImpl {
    MockKDsl.internalStaticMockk(*cls.map { InternalPlatformDsl.classForName(it) as KClass<*> }.toTypedArray())
}

/**
 * Builds a mock for object.
 * To actually use it you need to call use or mock/unmock.
 */
inline fun objectMockk(vararg objs: Any, recordPrivateCalls: Boolean = false): MockKObjectScope = MockK.useImpl {
    MockKDsl.internalObjectMockk(objs, recordPrivateCalls = recordPrivateCalls)
}

/**
 * Builds a mock for a class.
 */
inline fun <T : Any> classMockk(
    type: KClass<T>,
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalClassMockk(type, name, relaxed, *moreInterfaces, block = block)
}


object MockKAnnotations {
    /**
     * Initializes properties annotated with @MockK, @RelaxedMockK, @Slot and @SpyK in provided object.
     */
    inline fun init(vararg obj: Any) = MockK.useImpl {
        MockKDsl.internalInitAnnotatedMocks(obj.toList())
    }
}

expect object MockK {
    inline fun <T> useImpl(block: () -> T): T
}
