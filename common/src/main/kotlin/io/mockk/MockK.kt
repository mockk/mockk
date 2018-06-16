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
inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T, T> = MockK.useImpl {
    MockKDsl.internalEvery(stubBlock)
}

/**
 * Starts a block of stubbing for coroutines. Part of DSL.
 */
inline fun <T> coEvery(noinline stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T, T> = MockK.useImpl {
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
 * To actually use it you need to call use or mock/unmock.
 */
@Deprecated(
    message = "Scopes for mocking tend to be error prone. Use new 'mockkStatic' function",
    replaceWith = ReplaceWith(
        expression = "mockkStatic(T::class)",
        imports = ["io.mockk.mockkStatic"]
    )
)
inline fun <reified T : Any> staticMockk(): MockKStaticScope = MockK.useImpl {
    MockKDsl.internalStaticMockk<T>()
}

/**
 * Builds a static mock via static mock scope.
 * To actually use it you need to call use or mock/unmock/use.
 */
@Deprecated(
    message = "Scopes for mocking tend to be error prone. Use new 'mockkStatic' function",
    replaceWith = ReplaceWith(
        expression = "mockkStatic(cls)",
        imports = ["io.mockk.mockkStatic"]
    )
)
inline fun staticMockk(vararg cls: String): MockKStaticScope = MockK.useImpl {
    MockKDsl.internalStaticMockk(*cls.map { InternalPlatformDsl.classForName(it) as KClass<*> }.toTypedArray())
}

/**
 * Builds a mock for object.
 * To actually use it you need to call use or mock/unmock.
 */
@Deprecated(
    message = "Scopes for mocking tend to be error prone. Use new 'mockkObject' function",
    replaceWith = ReplaceWith(
        expression = "mockkObject(objs, recordPrivateCalls = recordPrivateCalls)",
        imports = ["io.mockk.mockkObject"]
    )
)
inline fun objectMockk(vararg objs: Any, recordPrivateCalls: Boolean = false): MockKObjectScope = MockK.useImpl {
    MockKDsl.internalObjectMockk(objs, recordPrivateCalls = recordPrivateCalls)
}

/**
 * Builds a mock using particular constructor.
 * To actually use it you need to call use or mock/unmock.
 */
@Deprecated(
    message = "Scopes for mocking tend to be error prone. Use new 'mockkConstructor' function",
    replaceWith = ReplaceWith(
        expression = "mockkConstructor(T::class, recordPrivateCalls = recordPrivateCalls, localToThread = localToThread)",
        imports = ["io.mockk.mockkConstructor"]
    )
)
inline fun <reified T : Any> constructorMockk(
    recordPrivateCalls: Boolean = false,
    localToThread: Boolean = false
): MockKConstructorScope<T> = MockK.useImpl {
    MockKDsl.internalConstructorMockk(recordPrivateCalls, localToThread)
}

/**
 * Builds a mock for a class.
 */
@Deprecated(
    message = "Every mocking function now starts with 'mockk...'. " +
            "Scoped functions alike objectMockk and staticMockk were error prone. " +
            "Use new 'mockkClass' function",
    replaceWith = ReplaceWith(
        expression = "mockkClass(type, name, relaxed, moreInterfaces, block)",
        imports = ["io.mockk.mockkClass"]
    )
)
inline fun <T : Any> classMockk(
    type: KClass<T>,
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalMockkClass(type, name, relaxed, *moreInterfaces, block = block)
}

inline fun <T : Any> mockkClass(
    type: KClass<T>,
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalMockkClass(type, name, relaxed, *moreInterfaces, block = block)
}

/**
 * Builds a static mock. Old static mocks of same classes are cancelled before.
 */
inline fun mockkObject(vararg objects: Any, recordPrivateCalls: Boolean = false) = MockK.useImpl {
    MockKDsl.internalMockkObject(*objects, recordPrivateCalls = recordPrivateCalls)
}

/**
 * Cancel object mocks.
 */
inline fun unmockkObject(vararg objects: Any) = MockK.useImpl {
    MockKDsl.internalUnmockkObject(*objects)
}

/**
 * Builds a static mock. Old static mocks of same classes are cancelled before.
 */
inline fun mockkStatic(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalMockkStatic(*classes)
}

/**
 * Builds a static mock. Old static mocks of same classes are cancelled before.
 */
inline fun mockkStatic(vararg classes: String) = MockK.useImpl {
    MockKDsl.internalMockkStatic(*classes.map { InternalPlatformDsl.classForName(it) as KClass<*> }.toTypedArray())
}

/**
 * Cancel static mocks.
 */
inline fun clearStaticMockk(
    vararg classes: KClass<*>,
    answers: Boolean = true,
    recordedCalls: Boolean = true,
    childMocks: Boolean = true
) = MockK.useImpl {
    MockKDsl.internalClearStaticMockk(
        *classes,
        answers = answers,
        recordedCalls = recordedCalls,
        childMocks = childMocks
    )
}

/**
 * Cancel static mocks.
 */
inline fun unmockkStatic(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalUnmockkStatic(*classes)
}

/**
 * Cancel static mocks.
 */
inline fun unmockkStatic(vararg classes: String) = MockK.useImpl {
    MockKDsl.internalUnmockkStatic(*classes.map { InternalPlatformDsl.classForName(it) as KClass<*> }.toTypedArray())
}

/**
 * Builds a constructor mock. Old constructor mocks of same classes are cancelled before.
 */
inline fun mockkConstructor(
    vararg classes: KClass<*>,
    recordPrivateCalls: Boolean = false,
    localToThread: Boolean = false
) = MockK.useImpl {
    MockKDsl.internalMockkConstructor(
        *classes,
        recordPrivateCalls = recordPrivateCalls,
        localToThread = localToThread
    )
}

/**
 * Cancel constructor mocks.
 */
inline fun unmockkConstructor(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalUnmockkConstructor(*classes)
}

/**
 * Clears constructor mock.
 */
inline fun clearConstructorMockk(
    vararg classes: KClass<*>,
    answers: Boolean = true,
    recordedCalls: Boolean = true,
    childMocks: Boolean = true
) = MockK.useImpl {
    MockKDsl.internalClearConstructorMockk(
        *classes,
        answers = answers,
        recordedCalls = recordedCalls,
        childMocks = childMocks
    )
}

/**
 * Cancels object, static and constructor mocks.
 */
inline fun unmockkAll() = MockK.useImpl {
    MockKDsl.internalUnmockkAll()
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
