@file:Suppress("NOTHING_TO_INLINE")

package io.mockk

import kotlin.reflect.KClass

/**
 * Builds a new mock for specified class
 *
 * @param name mock name
 * @param relaxed allows creation with no specific behaviour
 * @param moreInterfaces additional interfaces for this mockk to implement
 * @param relaxUnitFun allows creation with no specific behaviour for Unit function
 * @param block block to execute after mock is created with mock as a receiver
 *
 * @sample [io.mockk.MockKSamples.basicMockkCreation]
 * @sample [io.mockk.MockKSamples.mockkWithCreationBlock]
 */
inline fun <reified T : Any> mockk(
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    relaxUnitFun: Boolean = false,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalMockk(
        name,
        relaxed,
        *moreInterfaces,
        relaxUnitFun = relaxUnitFun,
        block = block
    )
}

/**
 * Builds a new spy for specified class. Initializes object via default constructor.
 *
 * A spy is a special kind of mockk that enables a mix of mocked behaviour and real behaviour.
 * A part of the behaviour may be mocked, but any non-mocked behaviour will call the original method.
 *
 * @param name spyk name
 * @param moreInterfaces additional interfaces for this spyk to implement
 * @param recordPrivateCalls allows this spyk to record any private calls, enabling a verification
 * @param block block to execute after spyk is created with spyk as a receiver
 *
 * @sample [io.mockk.SpykSamples.spyOriginalBehaviourDefaultConstructor]
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
 * Builds a new spy for specified class, copying fields from [objToCopy].
 *
 * A spy is a special kind of mockk that enables a mix of mocked behaviour and real behaviour.
 * A part of the behaviour may be mocked, but any non-mocked behaviour will call the original method.
 *
 * @sample [io.mockk.SpykSamples.spyOriginalBehaviourCopyingFields]
 * @sample [io.mockk.SpykSamples.spyOriginalBehaviourWithPrivateCalls]
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
 *
 * @sample [io.mockk.SlotSample.captureSlot]
 */
inline fun <reified T : Any> slot() = MockK.useImpl {
    MockKDsl.internalSlot<T>()
}

/**
 * Starts a block of stubbing. Part of DSL.
 *
 * Used to define what behaviour is going to be mocked.
 *
 * @sample [io.mockk.EverySample.simpleEvery]
 */
inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T, T> = MockK.useImpl {
    MockKDsl.internalEvery(stubBlock)
}

/**
 * Starts a block of stubbing for coroutines. Part of DSL.
 * Similar to [every]
 *
 * Used to define what behaviour is going to be mocked.
 * @see [every]
 */
inline fun <T> coEvery(noinline stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T, T> = MockK.useImpl {
    MockKDsl.internalCoEvery(stubBlock)
}

/**
 * Verifies that calls were made in the past. Part of DSL
 *
 * @param ordering how the verification should be ordered
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 * @param atLeast verifies that the behaviour happened at least [atLeast] times
 * @param atMost verifies that the behaviour happened at most [atMost] times
 * @param exactly verifies that the behaviour happened exactly [exactly] times. Use -1 to disable
 *
 * @sample [io.mockk.VerifySample.verifyAmount]
 * @sample [io.mockk.VerifySample.verifyRange]
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
 * Verifies that calls were made inside a coroutine.
 *
 * @param ordering how the verification should be ordered
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 * @param atLeast verifies that the behaviour happened at least [atLeast] times
 * @param atMost verifies that the behaviour happened at most [atMost] times
 * @param exactly verifies that the behaviour happened exactly [exactly] times. Use -1 to disable
 *
 * @see [verify]
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
 * Verifies that all calls inside [verifyBlock] happened, **not** verifying any order.
 *
 * If ordering is important, use [verifyOrder]
 *
 * @see verify
 * @see verifyOrder
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
inline fun verifyAll(
    inverse: Boolean = false,
    noinline verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyAll(inverse, verifyBlock)
}

/**
 * Verifies that all calls inside [verifyBlock] happened, checking that they happened in the order declared.
 *
 * @see verify
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 *
 * @sample [io.mockk.VerifySample.verifyOrder]
 * @sample [io.mockk.VerifySample.failingVerifyOrder]
 */
inline fun verifyOrder(
    inverse: Boolean = false,
    noinline verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyOrder(inverse, verifyBlock)
}

/**
 * Verifies that all calls inside [verifyBlock] happened, and no other call was made to those mocks
 *
 * @see verify
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 *
 * @sample [io.mockk.VerifySample.verifySequence]
 * @sample [io.mockk.VerifySample.failingVerifySequence]
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

/**
 * Builds a mock for an arbitrary class
 */
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
 * Builds an Object mock. Any mocks of this exact object are cancelled before it's mocked.
 *
 * @sample io.mockk.ObjectMockkSample.mockSimpleObject
 * @sample io.mockk.ObjectMockkSample.mockEnumeration
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
 * Builds a static mock. Any mocks of this exact class are cancelled before it's mocked
 *
 * @sample io.mockk.StaticMockkSample.mockJavaStatic
 */
inline fun mockkStatic(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalMockkStatic(*classes)
}

/**
 * Builds a static mock. Old static mocks of same classes are cancelled before.
 *
 * @sample io.mockk.StaticMockkSample.mockJavaStaticString
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
    inline fun init(
        vararg obj: Any,
        overrideRecordPrivateCalls: Boolean = false,
        relaxUnitFun: Boolean = false
    ) = MockK.useImpl {
        MockKDsl.internalInitAnnotatedMocks(obj.toList(), overrideRecordPrivateCalls, relaxUnitFun)
    }
}

expect object MockK {
    inline fun <T> useImpl(block: () -> T): T
}
