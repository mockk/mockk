@file:Suppress("NOTHING_TO_INLINE")

package io.mockk

import kotlin.reflect.KClass

/**
 * Builds a new mock for specified class.
 *
 * A mock is a fake version of a class that replaces all the methods with fake implementations.
 *
 * By default, every method that you wish to mock should be stubbed using [every].
 * Otherwise, it will throw when called, so you know if you forgot to mock a method.
 * If [relaxed] or [relaxUnitFun] is set to true, methods will automatically be stubbed.
 *
 * @param name mock name
 * @param relaxed allows creation with no specific behaviour. Unstubbed methods will not throw.
 * @param moreInterfaces additional interfaces for this mockk to implement, in addition to the specified class.
 * @param relaxUnitFun allows creation with no specific behaviour for Unit function.
 * Unstubbed methods that return [Unit] will not throw, while other methods will still throw unless they are stubbed.
 * @param block block to execute after mock is created with mock as a receiver. Similar to using [apply] on the mock object.
 *
 * @sample
 * interface Navigator {
 *   val currentLocation: String
 *   fun navigateTo(newLocation: String): Unit
 * }
 *
 * val navigator = mockk<Navigator>()
 * every { navigator.currentLocation } returns "Home"
 *
 * println(navigator.currentLocation) // prints "Home"
 * navigator.navigateTo("Store") // throws an error
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
        moreInterfaces,
        relaxUnitFun = relaxUnitFun,
        block = block
    )
}

/**
 * Builds a new spy for specified class. Initializes object via default constructor.
 *
 * A spy is a special kind of [mockk] that enables a mix of mocked behaviour and real behaviour.
 * A part of the behaviour may be mocked using [every], but any non-mocked behaviour will call the original method.
 *
 * @param name spyk name
 * @param moreInterfaces additional interfaces for this spyk to implement, in addition to the specified class.
 * @param recordPrivateCalls allows this spyk to record any private calls, enabling a verification.
 * @param block block to execute after spyk is created with spyk as a receiver. Similar to using [apply] on the spyk object.
 */
inline fun <reified T : Any> spyk(
    name: String? = null,
    vararg moreInterfaces: KClass<*>,
    recordPrivateCalls: Boolean = false,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalSpyk(
        name,
        moreInterfaces,
        recordPrivateCalls = recordPrivateCalls,
        block = block
    )
}

/**
 * Builds a new spy for specified class, copying fields from [objToCopy].
 *
 * A spy is a special kind of [mockk] that enables a mix of mocked behaviour and real behaviour.
 * A part of the behaviour may be mocked using [every], but any non-mocked behaviour will call the original method.
 *
 * @param name spyk name
 * @param moreInterfaces additional interfaces for this spyk to implement, in addition to the specified class.
 * @param recordPrivateCalls allows this spyk to record any private calls, enabling a verification.
 * @param block block to execute after spyk is created with spyk as a receiver. Similar to using [apply] on the spyk object.
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
        moreInterfaces,
        recordPrivateCalls = recordPrivateCalls,
        block = block
    )
}

/**
 * Creates new capturing slot.
 *
 * Slots allow you to capture what arguments a mocked method is called with.
 * When mocking a method using [every], pass the slot wrapped with the [MockKMatcherScope.capture] function in place of a method argument or [MockKMatcherScope.any].
 *
 * @sample
 * interface FileNetwork {
 *   fun download(name: String): File
 * }
 *
 * val network = mockk<FileNetwork>()
 * val slot = slot<String>()
 *
 * every { network.download(capture(slot)) } returns mockk()
 *
 * network.download("testfile")
 * // slot.captured is now "testfile"
 */
inline fun <reified T : Any?> slot() = MockK.useImpl {
    MockKDsl.internalSlot<T>()
}

/**
 * Starts a block of stubbing. Part of DSL.
 *
 * Used to define what behaviour is going to be mocked.
 *
 * @sample
 * interface Navigator {
 *   val currentLocation: String
 * }
 *
 * val navigator = mockk<Navigator>()
 * every { navigator.currentLocation } returns "Home"
 *
 * println(navigator.currentLocation) // prints "Home"
 * @see [coEvery] Coroutine version.
 */
fun <T> every(stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T, T> = MockK.useImpl {
    MockKDsl.internalEvery(stubBlock)
}

/**
 * Stub block to return Unit result. Part of DSL.
 *
 * Used to define what behaviour is going to be mocked.
 * Acts as a shortcut for `every { ... } returns Unit`.
 *
 * @see [every]
 * @see [coJustRun] Coroutine version.
 * @sample
 * every { logger.log(any()) } returns Unit
 * every { logger.log(any()) } just Runs
 * justRun { logger.log(any()) }
 */
fun justRun(stubBlock: MockKMatcherScope.() -> Unit) = every(stubBlock) just Runs

/**
 * Starts a block of stubbing for coroutines. Part of DSL.
 * Similar to [every], but works with suspend functions.
 *
 * Used to define what behaviour is going to be mocked.
 * @see [every]
 */
fun <T> coEvery(stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T, T> = MockK.useImpl {
    MockKDsl.internalCoEvery(stubBlock)
}

/**
 * Stub block to return Unit result as a coroutine block. Part of DSL.
 * Similar to [justRun], but works with suspend functions.
 *
 * Used to define what behaviour is going to be mocked.
 * Acts as a shortcut for `coEvery { ... } returns Unit`.
 * @see [justRun]
 */
fun coJustRun(stubBlock: suspend MockKMatcherScope.() -> Unit) = coEvery(stubBlock) just Runs

/**
 * Stub block to never return. Part of DSL.
 *
 * Used to define what behaviour is going to be mocked.
 * @see [coJustRun]
 */
fun coJustAwait(stubBlock: suspend MockKMatcherScope.() -> Unit) = coEvery(stubBlock) just Awaits

/**
 * Verifies that calls were made in the past. Part of DSL.
 *
 * @param ordering how the verification should be ordered
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 * @param atLeast verifies that the behaviour happened at least [atLeast] times
 * @param atMost verifies that the behaviour happened at most [atMost] times
 * @param exactly verifies that the behaviour happened exactly [exactly] times. Use -1 to disable
 * @param timeout timeout value in milliseconds. Will wait until one of two following states: either verification is
 * passed or timeout is reached.
 * @param verifyBlock code block containing at least 1 call to verify
 *
 * @sample
 * val navigator = mockk<Navigator>(relaxed = true)
 *
 * navigator.navigateTo("Park")
 * verify { navigator.navigateTo(any()) }
 * @see [coVerify] Coroutine version
 */
fun verify(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    timeout: Long = 0,
    verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerify(ordering, inverse, atLeast, atMost, exactly, timeout, verifyBlock)
}

/**
 * Verifies that calls were made inside a coroutine.
 *
 * @param ordering how the verification should be ordered
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 * @param atLeast verifies that the behaviour happened at least [atLeast] times
 * @param atMost verifies that the behaviour happened at most [atMost] times
 * @param exactly verifies that the behaviour happened exactly [exactly] times. Use -1 to disable
 * @param timeout timeout value in milliseconds. Will wait until one of two following states: either verification is
 * passed or timeout is reached.
 * @param verifyBlock code block containing at least 1 call to verify
 *
 * @see [verify]
 */
fun coVerify(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    timeout: Long = 0,
    verifyBlock: suspend MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalCoVerify(
        ordering,
        inverse,
        atLeast,
        atMost,
        exactly,
        timeout,
        verifyBlock
    )
}

/**
 * Verifies that all calls inside [verifyBlock] happened. **Does not** verify any order.
 *
 * If ordering is important, use [verifyOrder].
 *
 * @see coVerifyAll Coroutine version
 * @see verify
 * @see verifyOrder
 * @see verifySequence
 * @see verifyCount
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
fun verifyAll(
    inverse: Boolean = false,
    verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyAll(inverse, verifyBlock)
}

/**
 * Verifies that all calls inside [verifyBlock] happened, checking that they happened in the order declared.
 *
 * @see coVerifyOrder Coroutine version
 * @see verify
 * @see verifyAll
 * @see verifySequence
 * @see verifyCount
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
fun verifyOrder(
    inverse: Boolean = false,
    verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifyOrder(inverse, verifyBlock)
}

/**
 * Verifies that all calls inside [verifyBlock] happened, and no other call was made to those mocks
 *
 * @see coVerifySequence Coroutine version
 * @see verify
 * @see verifyOrder
 * @see verifyAll
 * @see verifyCount
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
fun verifySequence(
    inverse: Boolean = false,
    verifyBlock: MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalVerifySequence(inverse, verifyBlock)
}

/**
 * Verifies that calls and their count
 *
 * @see coVerifyCount Coroutine version
 * @see verify
 * @see verifyOrder
 * @see verifyAll
 * @see verifySequence
 *
 */
fun verifyCount(verifyBlock: MockKCallCountVerificationScope.() -> Unit) = MockK.useImpl {
    MockKCallCountVerificationScope().verifyBlock()
}

/**
 * Verifies that all calls inside [verifyBlock] happened. **Does not** verify any order. Coroutine version
 *
 * If ordering is important, use [coVerifyOrder]
 *
 * @see verifyAll
 * @see coVerify
 * @see coVerifyOrder
 * @see coVerifySequence
 * @see coVerifyCount
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
fun coVerifyAll(
    inverse: Boolean = false,
    verifyBlock: suspend MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalCoVerifyAll(inverse, verifyBlock)
}

/**
 * Verifies that all calls inside [verifyBlock] happened, checking that they happened in the order declared.
 * Coroutine version.
 *
 * @see verifyOrder
 * @see coVerify
 * @see coVerifyAll
 * @see coVerifySequence
 * @see coVerifyCount
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
fun coVerifyOrder(
    inverse: Boolean = false,
    verifyBlock: suspend MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalCoVerifyOrder(inverse, verifyBlock)
}

/**
 * Verifies that all calls inside [verifyBlock] happened, and no other call was made to those mocks.
 * Coroutine version.
 *
 * @see verifySequence
 * @see coVerify
 * @see coVerifyOrder
 * @see coVerifyAll
 * @see coVerifyCount
 *
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 */
fun coVerifySequence(
    inverse: Boolean = false,
    verifyBlock: suspend MockKVerificationScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalCoVerifySequence(inverse, verifyBlock)
}

/**
 * Verifies that calls and their count. Coroutine version
 *
 * @see verifyCount
 * @see coVerify
 * @see coVerifyOrder
 * @see coVerifyAll
 * @see coVerifySequence
 *
 */
fun coVerifyCount(verifyBlock: MockKCallCountCoVerificationScope.() -> Unit) = MockK.useImpl {
    MockKCallCountCoVerificationScope().verifyBlock()
}

/**
 * Exclude calls from recording
 *
 * @param current if current recorded calls should be filtered out
 * @see [coExcludeRecords] Coroutine version.
 */
fun excludeRecords(
    current: Boolean = true,
    excludeBlock: MockKMatcherScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalExcludeRecords(current, excludeBlock)
}

/**
 * Exclude calls from recording for a `suspend` block
 *
 * @param current if current recorded calls should be filtered out
 * @see [excludeRecords]
 */
fun coExcludeRecords(
    current: Boolean = true,
    excludeBlock: suspend MockKMatcherScope.() -> Unit
) = MockK.useImpl {
    MockKDsl.internalCoExcludeRecords(current, excludeBlock)
}

/**
 * Checks if all recorded calls were verified.
 */
fun confirmVerified(vararg mocks: Any) = MockK.useImpl {
    MockKDsl.internalConfirmVerified(mocks)
}

/**
 * Checks if all recorded calls are necessary.
 */
fun checkUnnecessaryStub(vararg mocks: Any) = MockK.useImpl {
    MockKDsl.internalCheckUnnecessaryStub(mocks)
}

/**
 * Resets information associated with specified mocks.
 * To clear all mocks use clearAllMocks.
 */
fun clearMocks(
    firstMock: Any,
    vararg mocks: Any,
    answers: Boolean = true,
    recordedCalls: Boolean = true,
    childMocks: Boolean = true,
    verificationMarks: Boolean = true,
    exclusionRules: Boolean = true
) =
    MockK.useImpl {
        MockKDsl.internalClearMocks(
            firstMock = firstMock,
            mocks = mocks,
            answers = answers,
            recordedCalls = recordedCalls,
            childMocks = childMocks,
            verificationMarks = verificationMarks,
            exclusionRules = exclusionRules
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
 * Builds a mock for an arbitrary class
 */
inline fun <T : Any> mockkClass(
    type: KClass<T>,
    name: String? = null,
    relaxed: Boolean = false,
    vararg moreInterfaces: KClass<*>,
    relaxUnitFun: Boolean = false,
    block: T.() -> Unit = {}
): T = MockK.useImpl {
    MockKDsl.internalMockkClass(
        type,
        name,
        relaxed,
        moreInterfaces,
        relaxUnitFun = relaxUnitFun,
        block = block
    )
}

/**
 * Builds an Object mock. Any mocks of this exact object are cancelled before it's mocked.
 *
 * This lets you mock object methods with [every].
 *
 * @sample
 * object CalculatorObject {
 *   fun add(a: Int, b: Int) = a + b
 * }
 *
 * mockkObject(CalculatorObject)
 * every { ObjBeingMocked.add(1, 2) } returns 55
 *
 * @see [unmockkObject] To manually cancel mock
 */
inline fun mockkObject(vararg objects: Any, recordPrivateCalls: Boolean = false) = MockK.useImpl {
    MockKDsl.internalMockkObject(objects, recordPrivateCalls = recordPrivateCalls)
}

/**
 * Cancel object mocks.
 */
inline fun unmockkObject(vararg objects: Any) = MockK.useImpl {
    MockKDsl.internalUnmockkObject(objects)
}

/**
 * Builds a static mock and unmocks it after the block has been executed.
 */
inline fun mockkObject(vararg objects: Any, recordPrivateCalls: Boolean = false, block: () -> Unit) {
    mockkObject(*objects, recordPrivateCalls = recordPrivateCalls)
    try {
        block()
    } finally {
        unmockkObject(*objects)
    }
}

/**
 * Builds a static mock. Any mocks of this exact class are cancelled before it's mocked
 *
 * @see [unmockkStatic] To manually cancel mock
 */
inline fun mockkStatic(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalMockkStatic(classes)
}

/**
 * Builds a static mock. Old static mocks of same classes are cancelled before.
 *
 * @see [unmockkStatic] To manually cancel mock
 */
inline fun mockkStatic(vararg classes: String) = MockK.useImpl {
    MockKDsl.internalMockkStatic(classes.map { InternalPlatformDsl.classForName(it) as KClass<*> }.toTypedArray())
}

/**
 * Clears static mocks.
 */
inline fun clearStaticMockk(
    vararg classes: KClass<*>,
    answers: Boolean = true,
    recordedCalls: Boolean = true,
    childMocks: Boolean = true
) = MockK.useImpl {
    MockKDsl.internalClearStaticMockk(
        classes,
        answers = answers,
        recordedCalls = recordedCalls,
        childMocks = childMocks
    )
}

/**
 * Cancel static mocks.
 */
inline fun unmockkStatic(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalUnmockkStatic(classes)
}

/**
 * Cancel static mocks.
 */
inline fun unmockkStatic(vararg classes: String) = MockK.useImpl {
    MockKDsl.internalUnmockkStatic(classes.map { InternalPlatformDsl.classForName(it) as KClass<*> }.toTypedArray())
}

/**
 * Builds a static mock and unmocks it after the block has been executed.
 */
inline fun mockkStatic(vararg classes: KClass<*>, block: () -> Unit) {
    mockkStatic(*classes)
    try {
        block()
    } finally {
        unmockkStatic(*classes)
    }
}

/**
 * Builds a static mock and unmocks it after the block has been executed.
 */
inline fun mockkStatic(vararg classes: String, block: () -> Unit) {
    mockkStatic(*classes)
    try {
        block()
    } finally {
        unmockkStatic(*classes)
    }
}

/**
 * Builds a constructor mock. Old constructor mocks of same classes are cancelled before.
 *
 * Once used, every constructor of the given class will start returning a singleton that can be mocked.
 * Rather than building a new instance every time the constructor is called,
 * MockK generates a singleton and always returns the same instance.
 * This will apply to all constructors for a given class, there is no way to distinguish between them.
 *
 * @see [unmockkConstructor] To manually cancel mock
 * @sample
 * class ClassToTest {
 *   private val log = Logger()
 * }
 *
 * mockkConstructor(Logger::class)
 * // ClassToTest.log will now use a mock instance
 */
inline fun mockkConstructor(
    vararg classes: KClass<*>,
    recordPrivateCalls: Boolean = false,
    localToThread: Boolean = false
) = MockK.useImpl {
    MockKDsl.internalMockkConstructor(
        classes,
        recordPrivateCalls = recordPrivateCalls,
        localToThread = localToThread
    )
}

/**
 * Cancel constructor mocks.
 */
inline fun unmockkConstructor(vararg classes: KClass<*>) = MockK.useImpl {
    MockKDsl.internalUnmockkConstructor(classes)
}

/**
 * Builds a constructor mock and unmocks it after the block has been executed.
 */
inline fun mockkConstructor(
    vararg classes: KClass<*>,
    recordPrivateCalls: Boolean = false,
    localToThread: Boolean = false,
    block: () -> Unit
) {
    mockkConstructor(*classes, recordPrivateCalls = recordPrivateCalls, localToThread = localToThread)
    try {
        block()
    } finally {
        unmockkConstructor(*classes)
    }
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
        classes,
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

/**
 * Clears all regular, object, static and constructor mocks.
 */
inline fun clearAllMocks(
    answers: Boolean = true,
    recordedCalls: Boolean = true,
    childMocks: Boolean = true,
    regularMocks: Boolean = true,
    objectMocks: Boolean = true,
    staticMocks: Boolean = true,
    constructorMocks: Boolean = true,
    currentThreadOnly: Boolean = false
) = MockK.useImpl {
    MockKDsl.internalClearAllMocks(
        answers,
        recordedCalls,
        childMocks,
        regularMocks,
        objectMocks,
        staticMocks,
        constructorMocks,
        currentThreadOnly=currentThreadOnly
    )
}

/**
 * Checks if provided mock is mock of certain type
 */
fun isMockKMock(
    mock: Any,
    regular: Boolean = true,
    spy: Boolean = false,
    objectMock: Boolean = false,
    staticMock: Boolean = false,
    constructorMock: Boolean = false
) = MockK.useImpl {
    MockKDsl.internalIsMockKMock(
        mock,
        regular,
        spy,
        objectMock,
        staticMock,
        constructorMock
    )
}


object MockKAnnotations {
    /**
     * Initializes properties annotated with @MockK, @RelaxedMockK, @Slot and @SpyK in provided object.
     */
    inline fun init(
        vararg obj: Any,
        overrideRecordPrivateCalls: Boolean = false,
        relaxUnitFun: Boolean = false,
        relaxed: Boolean = false
    ) = MockK.useImpl {
        MockKDsl.internalInitAnnotatedMocks(
            obj.toList(),
            overrideRecordPrivateCalls,
            relaxUnitFun,
            relaxed
        )
    }
}

expect object MockK {
    inline fun <T> useImpl(block: () -> T): T
}
