@file:Suppress("DEPRECATION", "NOTHING_TO_INLINE")

package io.mockk

import io.mockk.InternalPlatformDsl.toStr
import io.mockk.MockKGateway.*
import io.mockk.core.ValueClassSupport.boxedClass
import kotlinx.coroutines.awaitCancellation
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass

/**
 * Exception thrown by library
 */
class MockKException(message: String, ex: Throwable? = null) : RuntimeException(message, ex)


/**
 * DSL entry points.
 */
@Suppress("NOTHING_TO_INLINE")
object MockKDsl {
    /**
     * Builds a new mock for specified class
     */
    inline fun <reified T : Any> internalMockk(
        name: String? = null,
        relaxed: Boolean = false,
        vararg moreInterfaces: KClass<*>,
        relaxUnitFun: Boolean = false,
        block: T.() -> Unit = {}
    ): T {
        val mock = MockKGateway.implementation().mockFactory.mockk(
            T::class,
            name,
            relaxed,
            moreInterfaces,
            relaxUnitFun
        )
        block(mock)
        return mock
    }

    /**
     * Builds a new spy for specified class. Initializes object via default constructor.
     */
    inline fun <T : Any> internalSpyk(
        objToCopy: T,
        name: String? = null,
        vararg moreInterfaces: KClass<*>,
        recordPrivateCalls: Boolean = false,
        block: T.() -> Unit = {}
    ): T {
        val spy = MockKGateway.implementation().mockFactory.spyk(
            null,
            objToCopy,
            name,
            moreInterfaces,
            recordPrivateCalls
        )
        block(spy)
        return spy
    }

    /**
     * Builds a new spy for specified class. Copies fields from provided object
     */
    inline fun <reified T : Any> internalSpyk(
        name: String? = null,
        vararg moreInterfaces: KClass<*>,
        recordPrivateCalls: Boolean = false,
        block: T.() -> Unit = {}
    ): T {
        val spy = MockKGateway.implementation().mockFactory.spyk(
            T::class,
            null,
            name,
            moreInterfaces,
            recordPrivateCalls
        )
        block(spy)
        return spy
    }

    /**
     * Creates new capturing slot
     */
    inline fun <reified T : Any> internalSlot() = CapturingSlot<T>()

    /**
     * Starts a block of stubbing. Part of DSL.
     */
    fun <T> internalEvery(stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T, T> =
        MockKGateway.implementation().stubber.every(stubBlock, null)

    /**
     * Starts a block of stubbing for coroutines. Part of DSL.
     */
    fun <T> internalCoEvery(stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T, T> =
        MockKGateway.implementation().stubber.every(null, stubBlock)

    /**
     * Verifies calls happened in the past. Part of DSL
     */
    fun internalVerify(
        ordering: Ordering = Ordering.UNORDERED,
        inverse: Boolean = false,
        atLeast: Int = 1,
        atMost: Int = Int.MAX_VALUE,
        exactly: Int = -1,
        timeout: Long = 0,
        verifyBlock: MockKVerificationScope.() -> Unit
    ) {

        internalCheckExactlyAtMostAtLeast(exactly, atLeast, atMost, ordering)

        val min = if (exactly != -1) exactly else atLeast
        val max = if (exactly != -1) exactly else atMost

        MockKGateway.implementation().verifier.verify(
            VerificationParameters(ordering, min, max, inverse, timeout),
            verifyBlock,
            null
        )
    }

    /**
     * Verify for coroutines
     */
    fun internalCoVerify(
        ordering: Ordering = Ordering.UNORDERED,
        inverse: Boolean = false,
        atLeast: Int = 1,
        atMost: Int = Int.MAX_VALUE,
        exactly: Int = -1,
        timeout: Long = 0,
        verifyBlock: suspend MockKVerificationScope.() -> Unit
    ) {

        internalCheckExactlyAtMostAtLeast(exactly, atLeast, atMost, ordering)

        val min = if (exactly != -1) exactly else atLeast
        val max = if (exactly != -1) exactly else atMost

        MockKGateway.implementation().verifier.verify(
            VerificationParameters(ordering, min, max, inverse, timeout),
            null,
            verifyBlock
        )
    }

    @PublishedApi
    internal fun internalCheckExactlyAtMostAtLeast(exactly: Int, atLeast: Int, atMost: Int, ordering: Ordering) {
        if (exactly != -1 && (atLeast != 1 || atMost != Int.MAX_VALUE)) {
            throw MockKException("specify either atLeast/atMost or exactly")
        }
        if (exactly < -1) {
            throw MockKException("exactly should be positive")
        }
        if (atLeast < 0) {
            throw MockKException("atLeast should be positive")
        }
        if (atMost < 0) {
            throw MockKException("atMost should be positive")
        }
        if (atLeast > atMost) {
            throw MockKException("atLeast should less or equal atMost")
        }

        if (ordering != Ordering.UNORDERED) {
            if (atLeast != 1 || atMost != Int.MAX_VALUE || exactly != -1) {
                throw MockKException("atLeast, atMost, exactly is only allowed in unordered verify block")
            }
        }
    }

    /**
     * Shortcut for ordered calls verification
     */
    fun internalVerifyOrder(
        inverse: Boolean = false,
        verifyBlock: MockKVerificationScope.() -> Unit
    ) {
        internalVerify(Ordering.ORDERED, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Shortcut for all calls verification
     */
    fun internalVerifyAll(
        inverse: Boolean = false,
        verifyBlock: MockKVerificationScope.() -> Unit
    ) {
        internalVerify(Ordering.ALL, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Shortcut for sequence calls verification
     */
    fun internalVerifySequence(
        inverse: Boolean = false,
        verifyBlock: MockKVerificationScope.() -> Unit
    ) {
        internalVerify(Ordering.SEQUENCE, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Shortcut for ordered calls verification
     */
    fun internalCoVerifyOrder(
        inverse: Boolean = false,
        verifyBlock: suspend MockKVerificationScope.() -> Unit
    ) {
        internalCoVerify(Ordering.ORDERED, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Shortcut for all calls verification
     */
    fun internalCoVerifyAll(
        inverse: Boolean = false,
        verifyBlock: suspend MockKVerificationScope.() -> Unit
    ) {
        internalCoVerify(Ordering.ALL, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Shortcut for sequence calls verification
     */
    fun internalCoVerifySequence(
        inverse: Boolean = false,
        verifyBlock: suspend MockKVerificationScope.() -> Unit
    ) {
        internalCoVerify(Ordering.SEQUENCE, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Exclude calls from recording
     *
     * @param current if current recorded calls should be filtered out
     */
    fun internalExcludeRecords(
        current: Boolean = true,
        excludeBlock: MockKMatcherScope.() -> Unit
    ) {
        MockKGateway.implementation().excluder.exclude(
            ExclusionParameters(current),
            excludeBlock,
            null
        )
    }

    /**
     * Exclude calls from recording for a suspend block
     *
     * @param current if current recorded calls should be filtered out
     */
    fun internalCoExcludeRecords(
        current: Boolean = true,
        excludeBlock: suspend MockKMatcherScope.() -> Unit
    ) {
        MockKGateway.implementation().excluder.exclude(
            ExclusionParameters(current),
            null,
            excludeBlock
        )
    }

    /**
     * Checks if all recorded calls were verified.
     */
    fun internalConfirmVerified(vararg mocks: Any) {
        if (mocks.isEmpty()) {
            MockKGateway.implementation().verificationAcknowledger.acknowledgeVerified()
        }

        for (mock in mocks) {
            MockKGateway.implementation().verificationAcknowledger.acknowledgeVerified(mock)
        }
    }

    /**
     * Checks if all recorded calls are necessary.
     */
    fun internalCheckUnnecessaryStub(vararg mocks: Any) {
        if (mocks.isEmpty()) {
            MockKGateway.implementation().verificationAcknowledger.checkUnnecessaryStub()
        }

        for (mock in mocks) {
            MockKGateway.implementation().verificationAcknowledger.checkUnnecessaryStub(mock)
        }
    }

    /**
     * Resets information associated with mock
     */
    inline fun internalClearMocks(
        firstMock: Any,
        vararg mocks: Any,
        answers: Boolean = true,
        recordedCalls: Boolean = true,
        childMocks: Boolean = true,
        verificationMarks: Boolean = true,
        exclusionRules: Boolean = true
    ) {
        MockKGateway.implementation().clearer.clear(
            arrayOf(firstMock, *mocks),
            MockKGateway.ClearOptions(
                answers,
                recordedCalls,
                childMocks,
                verificationMarks,
                exclusionRules
            )
        )
    }

    /**
     * Registers instance factory and returns object able to do deregistration.
     */
    inline fun <reified T : Any> internalRegisterInstanceFactory(noinline instanceFactory: () -> T): Deregisterable {
        val factoryObj = object : MockKGateway.InstanceFactory {
            override fun instantiate(cls: KClass<*>): Any? {
                if (T::class == cls) {
                    return instanceFactory()
                }
                return null
            }
        }

        MockKGateway.implementation().instanceFactoryRegistry.registerFactory(factoryObj)
        return object : Deregisterable {
            override fun deregister() {
                MockKGateway.implementation().instanceFactoryRegistry.deregisterFactory(factoryObj)
            }
        }
    }

    /**
     * Executes block of code with registering and unregistering instance factory.
     */
    inline fun <reified T : Any, R> internalWithInstanceFactory(noinline instanceFactory: () -> T, block: () -> R): R {
        return internalRegisterInstanceFactory(instanceFactory).use {
            block()
        }
    }

    /**
     * Declares static mockk. Deprecated
     */
    inline fun <reified T : Any> internalStaticMockk() = MockKStaticScope(T::class)

    /**
     * Declares static mockk. Deprecated
     */
    inline fun internalStaticMockk(vararg kClass: KClass<out Any>) = MockKStaticScope(*kClass)

    /**
     * Declares object mockk. Deprecated
     */
    inline fun internalObjectMockk(objs: Array<out Any>, recordPrivateCalls: Boolean = false) =
        MockKObjectScope(*objs, recordPrivateCalls = recordPrivateCalls)

    /**
     * Declares constructor mockk. Deprecated
     */
    inline fun <reified T : Any> internalConstructorMockk(
        recordPrivateCalls: Boolean = false,
        localToThread: Boolean = false
    ) =
        MockKConstructorScope(T::class, recordPrivateCalls, localToThread)

    /**
     * Builds a mock for a class. Deprecated
     */
    inline fun <T : Any> internalMockkClass(
        type: KClass<T>,
        name: String?,
        relaxed: Boolean,
        vararg moreInterfaces: KClass<*>,
        relaxUnitFun: Boolean = false,
        block: T.() -> Unit
    ): T {
        val mock = MockKGateway.implementation().mockFactory.mockk(type, name, relaxed, moreInterfaces, relaxUnitFun)
        block(mock)
        return mock
    }

    /**
     * Initializes
     */
    inline fun internalInitAnnotatedMocks(
        targets: List<Any>,
        overrideRecordPrivateCalls: Boolean = false,
        relaxUnitFun: Boolean = false,
        relaxed: Boolean = false
    ) =
        MockKGateway.implementation().mockInitializer.initAnnotatedMocks(
            targets,
            overrideRecordPrivateCalls,
            relaxUnitFun,
            relaxed
        )

    /**
     * Object mockk
     */
    inline fun internalMockkObject(vararg objects: Any, recordPrivateCalls: Boolean = false) {
        val factory = MockKGateway.implementation().objectMockFactory

        objects.forEach {
            val cancellation = factory.objectMockk(it, recordPrivateCalls)

            internalClearMocks(it)

            MockKCancellationRegistry
                .subRegistry(MockKCancellationRegistry.Type.OBJECT)
                .cancelPut(it, cancellation)
        }

    }

    /**
     * Cancel object mocks.
     */
    inline fun internalUnmockkObject(vararg objects: Any) {
        objects.forEach {
            MockKCancellationRegistry
                .subRegistry(MockKCancellationRegistry.Type.OBJECT)
                .cancel(it)
        }
    }

    /**
     * Clear object mocks.
     */
    inline fun internalClearObjectMockk(
        vararg objects: Any,
        answers: Boolean = true,
        recordedCalls: Boolean = true,
        childMocks: Boolean = true,
        verificationMarks: Boolean = true,
        exclusionRules: Boolean = true
    ) {
        for (obj in objects) {
            MockKGateway.implementation().objectMockFactory.clear(
                obj,
                MockKGateway.ClearOptions(
                    answers,
                    recordedCalls,
                    childMocks,
                    verificationMarks,
                    exclusionRules
                )
            )
        }
    }

    /**
     * Static mockk
     */
    inline fun internalMockkStatic(vararg classes: KClass<*>) {
        val factory = MockKGateway.implementation().staticMockFactory

        classes.forEach {
            val cancellation = factory.staticMockk(it)

            internalClearStaticMockk(it)

            MockKCancellationRegistry
                .subRegistry(MockKCancellationRegistry.Type.STATIC)
                .cancelPut(it, cancellation)
        }
    }

    /**
     * Cancel static mocks.
     */
    inline fun internalUnmockkStatic(vararg classes: KClass<*>) {
        classes.forEach {
            MockKCancellationRegistry
                .subRegistry(MockKCancellationRegistry.Type.STATIC)
                .cancel(it)
        }
    }

    /**
     * Clear static mocks.
     */
    inline fun internalClearStaticMockk(
        vararg classes: KClass<*>,
        answers: Boolean = true,
        recordedCalls: Boolean = true,
        childMocks: Boolean = true,
        verificationMarks: Boolean = true,
        exclusionRules: Boolean = true
    ) {
        for (type in classes) {
            MockKGateway.implementation().staticMockFactory.clear(
                type,
                MockKGateway.ClearOptions(
                    answers,
                    recordedCalls,
                    childMocks,
                    verificationMarks,
                    exclusionRules
                )
            )
        }
    }

    /**
     * Constructor mockk
     */
    inline fun internalMockkConstructor(
        vararg classes: KClass<*>,
        recordPrivateCalls: Boolean = false,
        localToThread: Boolean = true
    ) {
        val factory = MockKGateway.implementation().constructorMockFactory

        classes.forEach {
            val cancellation = factory.constructorMockk(it, recordPrivateCalls, localToThread)

            internalClearConstructorMockk(it)

            MockKCancellationRegistry
                .subRegistry(MockKCancellationRegistry.Type.CONSTRUCTOR)
                .cancelPut(it, cancellation)
        }
    }

    /**
     * Cancel constructor mocks.
     */
    inline fun internalUnmockkConstructor(vararg classes: KClass<*>) {
        classes.forEach {
            MockKGateway.implementation().constructorMockFactory.clear(
                it,
                MockKGateway.ClearOptions(
                    answers = true,
                    recordedCalls = true,
                    childMocks = true,
                    verificationMarks = true,
                    exclusionRules = true
                )
            )
            MockKCancellationRegistry
                .subRegistry(MockKCancellationRegistry.Type.CONSTRUCTOR)
                .cancel(it)
        }
    }

    /**
     * Clear constructor mocks.
     */
    inline fun internalClearConstructorMockk(
        vararg classes: KClass<*>,
        answers: Boolean = true,
        recordedCalls: Boolean = true,
        childMocks: Boolean = true,
        verificationMarks: Boolean = true,
        exclusionRules: Boolean = true
    ) {
        for (type in classes) {
            MockKGateway.implementation().constructorMockFactory.clear(
                type,
                MockKGateway.ClearOptions(
                    answers,
                    recordedCalls,
                    childMocks,
                    verificationMarks,
                    exclusionRules
                )
            )
        }
    }

    /**
     * Unmockk everything
     */
    inline fun internalUnmockkAll() {
        MockKCancellationRegistry.cancelAll()
    }

    inline fun internalClearAllMocks(
        answers: Boolean = true,
        recordedCalls: Boolean = true,
        childMocks: Boolean = true,
        regularMocks: Boolean = true,
        objectMocks: Boolean = true,
        staticMocks: Boolean = true,
        constructorMocks: Boolean = true,
        verificationMarks: Boolean = true,
        exclusionRules: Boolean = true
    ) {
        val options = MockKGateway.ClearOptions(
            answers,
            recordedCalls,
            childMocks,
            verificationMarks,
            exclusionRules
        )
        val implementation = MockKGateway.implementation()

        if (regularMocks) {
            implementation.clearer.clearAll(options)
        }
        if (objectMocks) {
            implementation.objectMockFactory.clearAll(options)
        }
        if (staticMocks) {
            implementation.staticMockFactory.clearAll(options)
        }
        if (constructorMocks) {
            implementation.constructorMockFactory.clearAll(options)
        }
    }

    /*
     * Checks if provided mock is mock of certain type
     */
    fun internalIsMockKMock(
        mock: Any,
        regular: Boolean = true,
        spy: Boolean = false,
        objectMock: Boolean = false,
        staticMock: Boolean = false,
        constructorMock: Boolean = false
    ): Boolean {
        val typeChecker = MockKGateway.implementation().mockTypeChecker

        return when {
            regular && typeChecker.isRegularMock(mock) -> true
            spy && typeChecker.isSpy(mock) -> true
            objectMock && typeChecker.isObjectMock(mock) -> true
            staticMock && typeChecker.isStaticMock(mock) -> true
            constructorMock && typeChecker.isConstructorMock(mock) -> true
            else -> false
        }
    }
}

/**
 * Verification ordering
 */
enum class Ordering {
    /**
     * Order is not important. Some calls just should happen
     */
    UNORDERED,

    /**
     * Order is not important. All calls should happen
     */
    ALL,

    /**
     * Order is important, but not all calls are checked
     */
    ORDERED,

    /**
     * Order is important and all calls should be specified
     */
    SEQUENCE
}


/**
 * Basic stub/verification scope. Part of DSL.
 *
 * Inside of the scope you can interact with mocks.
 * You can chain calls to the mock, put argument matchers instead of arguments,
 * capture arguments, combine matchers in and/or/not expressions.
 *
 * It's not required to specify all arguments as matchers,
 * if the argument value is constant it's automatically replaced with eq() matcher.
 * .
 * Handling arguments that have defaults fetched from function (alike System.currentTimeMillis())
 * can be an issue, because it's not a constant. Such arguments can always be replaced
 * with some matcher.
 *
 * Provided information is gathered and associated with mock
 */
open class MockKMatcherScope(
    @PublishedApi
    internal val callRecorder: CallRecorder,
    val lambda: CapturingSlot<Function<*>>
) {

    inline fun <reified T : Any> match(matcher: Matcher<T>): T {
        return callRecorder.matcher(matcher, T::class)
    }

    inline fun <reified T : Any> match(noinline matcher: (T) -> Boolean): T =
        match(FunctionMatcher(matcher, T::class))

    inline fun <reified T : Any> matchNullable(noinline matcher: (T?) -> Boolean): T =
        match(FunctionWithNullableArgMatcher(matcher, T::class))

    inline fun <reified T : Any> eq(value: T, inverse: Boolean = false): T =
        match(EqMatcher(value, inverse = inverse))

    inline fun <reified T : Any> neq(value: T): T = eq(value, true)
    inline fun <reified T : Any> refEq(value: T, inverse: Boolean = false): T =
        match(EqMatcher(value, ref = true, inverse = inverse))

    inline fun <reified T : Any> nrefEq(value: T) = refEq(value, true)

    inline fun <reified T : Any> any(): T = match(ConstantMatcher(true))
    inline fun <reified T : Any> capture(lst: MutableList<T>): T = match(CaptureMatcher(lst, T::class))
    inline fun <reified T : Any> capture(lst: CapturingSlot<T>): T = match(CapturingSlotMatcher(lst, T::class))
    inline fun <reified T : Any> captureNullable(lst: MutableList<T?>): T? =
        match(CaptureNullableMatcher(lst, T::class))

    inline fun <reified T : Comparable<T>> cmpEq(value: T): T = match(ComparingMatcher(value, 0, T::class))
    inline fun <reified T : Comparable<T>> more(value: T, andEquals: Boolean = false): T =
        match(ComparingMatcher(value, if (andEquals) 2 else 1, T::class))

    inline fun <reified T : Comparable<T>> less(value: T, andEquals: Boolean = false): T =
        match(ComparingMatcher(value, if (andEquals) -2 else -1, T::class))

    inline fun <reified T : Comparable<T>> range(
        from: T,
        to: T,
        fromInclusive: Boolean = true,
        toInclusive: Boolean = true
    ): T = and(more(from, fromInclusive), less(to, toInclusive))

    inline fun <reified T : Any> and(left: T, right: T): T = match(AndOrMatcher<T>(true, left, right))
    inline fun <reified T : Any> or(left: T, right: T): T = match(AndOrMatcher<T>(false, left, right))
    inline fun <reified T : Any> not(value: T): T = match(NotMatcher<T>(value))
    inline fun <reified T : Any> isNull(inverse: Boolean = false): T = match(NullCheckMatcher<T>(inverse))
    inline fun <reified T : Any, R : T> ofType(cls: KClass<R>): T = match(OfTypeMatcher<T>(cls))
    inline fun <reified T : Any> ofType(): T = match(OfTypeMatcher<T>(T::class))

    inline fun <reified T : Any> anyVararg() = varargAllNullable<T> { true }
    inline fun anyBooleanVararg() = anyVararg<Boolean>().toBooleanArray()
    inline fun anyByteVararg() = anyVararg<Byte>().toByteArray()
    inline fun anyCharVararg() = anyVararg<Char>().toCharArray()
    inline fun anyShortVararg() = anyVararg<Short>().toShortArray()
    inline fun anyIntVararg() = anyVararg<Int>().toIntArray()
    inline fun anyLongVararg() = anyVararg<Long>().toLongArray()
    inline fun anyFloatVararg() = anyVararg<Float>().toFloatArray()
    inline fun anyDoubleVararg() = anyVararg<Double>().toDoubleArray()

    inline fun <reified T : Any> varargAll(noinline matcher: MockKVarargScope.(T) -> Boolean) =
        varargAllNullable<T> {
            when (it) {
                null -> false
                else -> matcher(it)
            }
        }

    inline fun <reified T : Any> varargAllNullable(noinline matcher: MockKVarargScope.(T?) -> Boolean) =
        arrayOf(callRecorder.matcher(VarargMatcher(true, matcher), T::class))

    inline fun varargAllBoolean(noinline matcher: MockKVarargScope.(Boolean) -> Boolean) =
        varargAll(matcher).toBooleanArray()

    inline fun varargAllByte(noinline matcher: MockKVarargScope.(Byte) -> Boolean) =
        varargAll(matcher).toByteArray()

    inline fun varargAllChar(noinline matcher: MockKVarargScope.(Char) -> Boolean) =
        varargAll(matcher).toCharArray()

    inline fun varargAllShort(noinline matcher: MockKVarargScope.(Short) -> Boolean) =
        varargAll(matcher).toShortArray()

    inline fun varargAllInt(noinline matcher: MockKVarargScope.(Int) -> Boolean) =
        varargAll(matcher).toIntArray()

    inline fun varargAllLong(noinline matcher: MockKVarargScope.(Long) -> Boolean) =
        varargAll(matcher).toLongArray()

    inline fun varargAllFloat(noinline matcher: MockKVarargScope.(Float) -> Boolean) =
        varargAll(matcher).toFloatArray()

    inline fun varargAllDouble(noinline matcher: MockKVarargScope.(Double) -> Boolean) =
        varargAll(matcher).toDoubleArray()

    inline fun <reified T : Any> varargAny(noinline matcher: MockKVarargScope.(T) -> Boolean) =
        varargAnyNullable<T> {
            when (it) {
                null -> false
                else -> matcher(it)
            }
        }

    inline fun <reified T : Any> varargAnyNullable(noinline matcher: MockKVarargScope.(T?) -> Boolean) =
        arrayOf(callRecorder.matcher(VarargMatcher(false, matcher), T::class))

    inline fun varargAnyBoolean(noinline matcher: MockKVarargScope.(Boolean) -> Boolean) =
        varargAny(matcher).toBooleanArray()

    inline fun varargAnyByte(noinline matcher: MockKVarargScope.(Byte) -> Boolean) =
        varargAny(matcher).toByteArray()

    inline fun varargAnyChar(noinline matcher: MockKVarargScope.(Char) -> Boolean) =
        varargAny(matcher).toCharArray()

    inline fun varargAnyShort(noinline matcher: MockKVarargScope.(Short) -> Boolean) =
        varargAny(matcher).toShortArray()

    inline fun varargAnyInt(noinline matcher: MockKVarargScope.(Int) -> Boolean) =
        varargAny(matcher).toIntArray()

    inline fun varargAnyLong(noinline matcher: MockKVarargScope.(Long) -> Boolean) =
        varargAny(matcher).toLongArray()

    inline fun varargAnyFloat(noinline matcher: MockKVarargScope.(Float) -> Boolean) =
        varargAny(matcher).toFloatArray()

    inline fun varargAnyDouble(noinline matcher: MockKVarargScope.(Double) -> Boolean) =
        varargAny(matcher).toDoubleArray()

    class MockKVarargScope(val position: Int, val nArgs: Int)

    inline fun <reified T : () -> R, R> invoke() = match(InvokeMatcher<T> { it() })
    inline fun <reified T : (A1) -> R, R, A1> invoke(arg1: A1) = match(InvokeMatcher<T> { it(arg1) })
    inline fun <reified T : (A1, A2) -> R, R, A1, A2> invoke(arg1: A1, arg2: A2) =
        match(InvokeMatcher<T> { it(arg1, arg2) })

    inline fun <reified T : (A1, A2, A3) -> R, R, A1, A2, A3> invoke(arg1: A1, arg2: A2, arg3: A3) =
        match(InvokeMatcher<T> { it(arg1, arg2, arg3) })

    inline fun <reified T : (A1, A2, A3, A4) -> R, R, A1, A2, A3, A4> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4) =
        match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4) })

    inline fun <reified T : (A1, A2, A3, A4, A5) -> R, R, A1, A2, A3, A4, A5> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6) -> R, R, A1, A2, A3, A4, A5, A6> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7) -> R, R, A1, A2, A3, A4, A5, A6, A7> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13
    ) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13) })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16,
            arg17
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16,
            arg17,
            arg18
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16,
            arg17,
            arg18,
            arg19
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19,
        arg20: A20
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16,
            arg17,
            arg18,
            arg19,
            arg20
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19,
        arg20: A20,
        arg21: A21
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16,
            arg17,
            arg18,
            arg19,
            arg20,
            arg21
        )
    })

    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> invoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19,
        arg20: A20,
        arg21: A21,
        arg22: A22
    ) = match(InvokeMatcher<T> {
        it(
            arg1,
            arg2,
            arg3,
            arg4,
            arg5,
            arg6,
            arg7,
            arg8,
            arg9,
            arg10,
            arg11,
            arg12,
            arg13,
            arg14,
            arg15,
            arg16,
            arg17,
            arg18,
            arg19,
            arg20,
            arg21,
            arg22
        )
    })

    inline fun <reified T : suspend () -> R, R> coInvoke() =
        match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it() } })

    inline fun <reified T : suspend (A1) -> R, R, A1> coInvoke(arg1: A1) =
        match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1) } })

    inline fun <reified T : suspend (A1, A2) -> R, R, A1, A2> coInvoke(arg1: A1, arg2: A2) =
        match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1, arg2) } })

    inline fun <reified T : suspend (A1, A2, A3) -> R, R, A1, A2, A3> coInvoke(arg1: A1, arg2: A2, arg3: A3) =
        match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1, arg2, arg3) } })

    inline fun <reified T : suspend (A1, A2, A3, A4) -> R, R, A1, A2, A3, A4> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4
    ) = match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1, arg2, arg3, arg4) } })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5) -> R, R, A1, A2, A3, A4, A5> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5
    ) = match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1, arg2, arg3, arg4, arg5) } })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6) -> R, R, A1, A2, A3, A4, A5, A6> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6
    ) = match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6) } })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7) -> R, R, A1, A2, A3, A4, A5, A6, A7> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7
    ) = match(InvokeMatcher<T> { InternalPlatformDsl.runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7) } })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16,
                arg17
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16,
                arg17,
                arg18
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16,
                arg17,
                arg18,
                arg19
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19,
        arg20: A20
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16,
                arg17,
                arg18,
                arg19,
                arg20
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19,
        arg20: A20,
        arg21: A21
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16,
                arg17,
                arg18,
                arg19,
                arg20,
                arg21
            )
        }
    })

    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> coInvoke(
        arg1: A1,
        arg2: A2,
        arg3: A3,
        arg4: A4,
        arg5: A5,
        arg6: A6,
        arg7: A7,
        arg8: A8,
        arg9: A9,
        arg10: A10,
        arg11: A11,
        arg12: A12,
        arg13: A13,
        arg14: A14,
        arg15: A15,
        arg16: A16,
        arg17: A17,
        arg18: A18,
        arg19: A19,
        arg20: A20,
        arg21: A21,
        arg22: A22
    ) = match(InvokeMatcher<T> {
        InternalPlatformDsl.runCoroutine {
            it(
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
                arg6,
                arg7,
                arg8,
                arg9,
                arg10,
                arg11,
                arg12,
                arg13,
                arg14,
                arg15,
                arg16,
                arg17,
                arg18,
                arg19,
                arg20,
                arg21,
                arg22
            )
        }
    })

    inline fun <reified T : Any> allAny(): T = match(AllAnyMatcher())
    inline fun <reified T : Any> array(vararg matchers: Matcher<Any>): T = match(ArrayMatcher(matchers.toList()))

    @Suppress("NOTHING_TO_INLINE")
    inline fun <R, T : Any> R.hint(cls: KClass<T>, n: Int = 1): R {
        callRecorder.hintNextReturnType(cls, n)
        return this
    }

    /**
     * Captures lambda function. Captured lambda<(A1, A2, ...) -> R>().invoke(...) can be used in answer scope.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Function<*>> captureLambda(): T {
        val matcher = CapturingSlotMatcher(lambda as CapturingSlot<T>, T::class)
        return callRecorder.matcher(matcher, T::class)
    }

    /**
     * Captures coroutine. Captured coroutine<suspend (A1, A2, ...) -> R>().coInvoke(...) can be used in answer scope.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> captureCoroutine(): T {
        val matcher = CapturingSlotMatcher(lambda as CapturingSlot<T>, T::class)
        return callRecorder.matcher(matcher, T::class)
    }

    inline fun <reified T : Any> coMatch(noinline matcher: suspend (T) -> Boolean): T = match {
        InternalPlatformDsl.runCoroutine {
            matcher(it)
        }
    }

    inline fun <reified T : Any> coMatchNullable(noinline matcher: suspend (T?) -> Boolean): T = matchNullable {
        InternalPlatformDsl.runCoroutine {
            matcher(it)
        }
    }

    operator fun Any.get(name: String) =
        DynamicCall(this, name, { any() })

    infix fun Any.invoke(name: String) =
        DynamicCallLong(this, name, { any() })

    infix fun Any.invokeNoArgs(name: String) =
        invoke(name).withArguments(listOf())

    infix fun Any.getProperty(name: String) =
        InternalPlatformDsl.dynamicGet(this, name)

    infix fun Any.setProperty(name: String) = DynamicSetProperty(this, name)

    class DynamicSetProperty(val self: Any, val name: String) {
        infix fun value(value: Any?) {
            InternalPlatformDsl.dynamicSet(self, name, value)
        }
    }

    class DynamicCall(
        val self: Any,
        val methodName: String,
        val anyContinuationGen: () -> Continuation<*>
    ) {
        operator fun invoke(vararg args: Any?) =
            InternalPlatformDsl.dynamicCall(self, methodName, args, anyContinuationGen)
    }

    class DynamicCallLong(
        val self: Any,
        val methodName: String,
        val anyContinuationGen: () -> Continuation<*>
    ) {
        infix fun withArguments(args: List<Any?>) =
            InternalPlatformDsl.dynamicCall(self, methodName, args.toTypedArray(), anyContinuationGen)
    }

    inline fun <reified T : Any> anyConstructed(): T =
        MockKGateway.implementation().constructorMockFactory.mockPlaceholder(T::class)

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> constructedWith(vararg matchers: Matcher<*>): T =
        MockKGateway.implementation().constructorMockFactory.mockPlaceholder(
            T::class,
            args = matchers as Array<Matcher<*>>
        )
}

/**
 * Part of DSL. Additional operations for verification scope.
 */
class MockKVerificationScope(
    callRecorder: CallRecorder,
    lambda: CapturingSlot<Function<*>>
) : MockKMatcherScope(callRecorder, lambda) {
    inline fun <reified T : Any> withArg(noinline captureBlock: MockKAssertScope.(T) -> Unit): T = match {
        MockKAssertScope(it).captureBlock(it)
        true
    }

    inline fun <reified T : Any> withNullableArg(noinline captureBlock: MockKAssertScope.(T?) -> Unit): T =
        matchNullable {
            MockKAssertScope(it).captureBlock(it)
            true
        }

    inline fun <reified T : Any> coWithArg(noinline captureBlock: suspend MockKAssertScope.(T) -> Unit): T =
        withArg {
            InternalPlatformDsl.runCoroutine {
                captureBlock(it)
            }
        }

    inline fun <reified T : Any> coWithNullableArg(noinline captureBlock: suspend MockKAssertScope.(T?) -> Unit): T =
        withNullableArg {
            InternalPlatformDsl.runCoroutine {
                captureBlock(it)
            }
        }

    infix fun Any.wasNot(called: Called) {
        listOf(this) wasNot called
    }

    @Suppress("UNUSED_PARAMETER")
    infix fun List<Any>.wasNot(called: Called) {
        callRecorder.wasNotCalled(this)
    }
}

/**
 * Part of DSL. Object to represent phrase "wasNot Called"
 */
object Called
typealias called = Called

/**
 * Part of DSL. Scope for assertions on arguments during verifications.
 */
class MockKAssertScope(val actual: Any?)

fun MockKAssertScope.checkEquals(expected: Any?) {
    if (!InternalPlatformDsl.deepEquals(expected, actual)) {
        throw AssertionError(formatAssertMessage(actual, expected))
    }
}

fun MockKAssertScope.checkEquals(msg: String, expected: Any?) {
    if (!InternalPlatformDsl.deepEquals(expected, actual)) {
        throw AssertionError(formatAssertMessage(actual, expected, msg))
    }
}

private fun formatAssertMessage(actual: Any?, expected: Any?, message: String? = null): String {
    val msgFormatted = if (message != null) "$message " else ""

    return "${msgFormatted}expected [$expected] but found [$actual]"
}


/**
 * Part of DSL. Object to represent phrase "just Runs"
 */
object Runs
typealias runs = Runs

/**
 * Part of DSL. Object to represent phrase "just Awaits"
 */
object Awaits
typealias awaits = Awaits

/**
 * Stub scope. Part of DSL
 *
 * Allows to specify function result
 */
class MockKStubScope<T, B>(
    private val answerOpportunity: AnswerOpportunity<T>,
    private val callRecorder: CallRecorder,
    private val lambda: CapturingSlot<Function<*>>
) {
    infix fun answers(answer: Answer<T>): MockKAdditionalAnswerScope<T, B> {
        answerOpportunity.provideAnswer(answer)
        return MockKAdditionalAnswerScope(answerOpportunity, callRecorder, lambda)
    }

    infix fun returns(returnValue: T) = answers(ConstantAnswer(returnValue))

    infix fun returnsMany(values: List<T>) = answers(ManyAnswersAnswer(values.allConst()))

    fun returnsMany(vararg values: T) = returnsMany(values.toList())

    /**
     * Returns the nth argument of what has been called.
     */
    @Suppress("UNCHECKED_CAST")
    infix fun returnsArgument(n: Int): MockKAdditionalAnswerScope<T, B> =
        this answers { invocation.args[n] as T }

    infix fun throws(ex: Throwable) = answers(ThrowingAnswer(ex))

    infix fun throwsMany(exList: List<Throwable>): MockKAdditionalAnswerScope<T, B> =
        this answers (ManyAnswersAnswer(exList.map { ThrowingAnswer(it) }))

    infix fun answers(answer: MockKAnswerScope<T, B>.(Call) -> T) =
        answers(FunctionAnswer { MockKAnswerScope<T, B>(lambda, it).answer(it) })

    @Suppress("UNUSED_PARAMETER")
    infix fun <K : Any> propertyType(cls: KClass<K>) = MockKStubScope<T, K>(answerOpportunity, callRecorder, lambda)

    @Suppress("UNUSED_PARAMETER")
    infix fun <K : Any> nullablePropertyType(cls: KClass<K>) =
        MockKStubScope<T, K?>(answerOpportunity, callRecorder, lambda)

    infix fun coAnswers(answer: suspend MockKAnswerScope<T, B>.(Call) -> T) =
        answers(CoFunctionAnswer { MockKAnswerScope<T, B>(lambda, it).answer(it) })
}

/**
 * Part of DSL. Answer placeholder for Unit returning functions.
 */
@Suppress("UNUSED_PARAMETER")
infix fun MockKStubScope<Unit, Unit>.just(runs: Runs) = answers(ConstantAnswer(Unit))

/**
 * Part of DSL. Answer placeholder for never returning suspend functions.
 */
@Suppress("UNUSED_PARAMETER")
infix fun <T, B> MockKStubScope<T, B>.just(awaits: Awaits) = coAnswers { awaitCancellation() }

/**
 * Scope to chain additional answers to reply. Part of DSL
 */
class MockKAdditionalAnswerScope<T, B>(
    private val answerOpportunity: AnswerOpportunity<T>,
    private val callRecorder: CallRecorder,
    private val lambda: CapturingSlot<Function<*>>
) {
    infix fun andThenAnswer(answer: Answer<T>): MockKAdditionalAnswerScope<T, B> {
        answerOpportunity.provideAnswer(answer)
        return this
    }

    infix fun andThenAnswer(answer: MockKAnswerScope<T, B>.(Call) -> T) =
        andThenAnswer(FunctionAnswer { MockKAnswerScope<T, B>(lambda, it).answer(it) })

    infix fun andThen(returnValue: T) = andThenAnswer(ConstantAnswer(returnValue))

    infix fun andThenMany(values: List<T>) = andThenAnswer(ManyAnswersAnswer(values.allConst()))

    fun andThenMany(vararg values: T) = andThenMany(values.toList())

    infix fun andThenThrows(ex: Throwable) = andThenAnswer(ThrowingAnswer(ex))

    infix fun andThenThrowsMany(exList: List<Throwable>) =
        andThenAnswer(ManyAnswersAnswer(exList.map { ThrowingAnswer(it) }))

    @Deprecated("Use andThenAnswer instead of andThen.")
    infix fun andThen(answer: MockKAnswerScope<T, B>.(Call) -> T) =
        andThenAnswer(FunctionAnswer { MockKAnswerScope<T, B>(lambda, it).answer(it) })

    infix fun coAndThen(answer: suspend MockKAnswerScope<T, B>.(Call) -> T) =
        andThenAnswer(CoFunctionAnswer { MockKAnswerScope<T, B>(lambda, it).answer(it) })
}

/**
 * Part of DSL. Answer placeholder for Unit returning functions.
 */
@Suppress("UNUSED_PARAMETER")
infix fun MockKAdditionalAnswerScope<Unit, Unit>.andThenJust(runs: Runs) = andThenAnswer(ConstantAnswer(Unit))

/**
 * Part of DSL. Answer placeholder for never returning functions.
 */
@Suppress("UNUSED_PARAMETER")
infix fun <T, B> MockKAdditionalAnswerScope<T, B>.andThenJust(awaits: Awaits) = coAndThen { awaitCancellation() }

internal fun <T> List<T>.allConst() = this.map { ConstantAnswer(it) }

/**
 * Scope for answering functions. Part of DSL
 */
class MockKAnswerScope<T, B>(
    @PublishedApi
    internal val lambda: CapturingSlot<Function<*>>,
    val call: Call
) {

    val invocation = call.invocation
    val matcher = call.matcher

    val self
        get() = invocation.self

    val method
        get() = invocation.method

    val args
        get() = invocation.args

    val nArgs
        get() = invocation.args.size

    inline fun <reified T> firstArg() = invocation.args[0] as T
    inline fun <reified T> secondArg() = invocation.args[1] as T
    inline fun <reified T> thirdArg() = invocation.args[2] as T
    inline fun <reified T> lastArg() = invocation.args.last() as T
    inline fun <reified T> arg(n: Int) = invocation.args[n] as T

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T> MutableList<T>.captured() = last()

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Function<*>> lambda() = lambda as CapturingSlot<T>

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> coroutine() = lambda as CapturingSlot<T>

    val nothing = null

    @Suppress("UNCHECKED_CAST")
    fun callOriginal(): T = call.invocation.originalCall.invoke() as T

    @Suppress("UNCHECKED_CAST")
    val value: B
        get() = valueAny as B

    val valueAny: Any?
        get() = firstArg()

    private val backingFieldValue: BackingFieldValue? by lazy { call.fieldValueProvider() }

    @Suppress("UNCHECKED_CAST")
    var fieldValue: B
        set(value) {
            fieldValueAny = value
        }
        get() = fieldValueAny as B

    var fieldValueAny: Any?
        set(value) {
            val fv = backingFieldValue
                ?: throw MockKException("no backing field found for '${call.invocation.method.name}'")

            fv.setter(value)
        }
        get() {
            val fv = backingFieldValue
                ?: throw MockKException("no backing field found for '${call.invocation.method.name}'")
            return fv.getter()
        }
}

/**
 * Cancelable mocking scope
 */
abstract class MockKUnmockKScope {
    fun mock() {
        val cancellation = doMock()
        MockKCancellationRegistry.pushCancellation(cancellation)
    }

    fun unmock() {
        val cancellation = MockKCancellationRegistry.popCancellation() ?: throw MockKException("Not mocked")
        cancellation.invoke()

    }

    protected abstract fun doMock(): () -> Unit

    abstract fun clear(
        answers: Boolean = true,
        recordedCalls: Boolean = true,
        childMocks: Boolean = true,
        verificationMarks: Boolean = true,
        exclusionRules: Boolean = true
    )

    operator fun plus(scope: MockKUnmockKScope): MockKUnmockKScope = MockKUnmockKCompositeScope(this, scope)
}

typealias MockKCancellation = () -> Unit

object MockKCancellationRegistry {
    enum class Type { OBJECT, STATIC, CONSTRUCTOR }
    class RegistryPerType {
        private val mapTl = InternalPlatformDsl.threadLocal { mutableMapOf<Any, MockKCancellation>() }

        fun cancelPut(key: Any, newCancellation: MockKCancellation) {
            val map = mapTl.value
            map.remove(key)?.invoke()
            map[key] = newCancellation
        }

        fun cancelAll() {
            val map = mapTl.value
            map.values.forEach { it() }
            map.clear()
        }

        fun cancel(key: Any) {
            mapTl.value.remove(key)?.invoke()
        }
    }

    private val stack = InternalPlatformDsl.threadLocal { mutableListOf<MockKCancellation>() }
    private val perType = mapOf(
        Type.OBJECT to RegistryPerType(),
        Type.STATIC to RegistryPerType(),
        Type.CONSTRUCTOR to RegistryPerType()
    )

    fun subRegistry(type: Type) = perType[type]!!

    fun pushCancellation(cancellation: MockKCancellation) = stack.value.add(cancellation)
    fun popCancellation(): (MockKCancellation)? {
        val list = stack.value
        return if (list.isEmpty())
            null
        else
            list.removeAt(list.size - 1)
    }

    fun cancelAll() {
        stack.value.apply { forEach { it() } }.clear()
        perType.values.forEach { it.cancelAll() }
    }
}

/**
 * Composite of two scopes. Part of DSL
 */
class MockKUnmockKCompositeScope(
    val first: MockKUnmockKScope,
    val second: MockKUnmockKScope
) : MockKUnmockKScope() {

    override fun doMock(): MockKCancellation {
        first.mock()
        second.mock()

        return {
            first.unmock()
            second.unmock()
        }
    }

    override fun clear(
        answers: Boolean,
        recordedCalls: Boolean,
        childMocks: Boolean,
        verificationMarks: Boolean,
        exclusionRules: Boolean
    ) {
        first.clear(
            answers,
            recordedCalls,
            childMocks,
            verificationMarks,
            exclusionRules
        )
        second.clear(
            answers,
            recordedCalls,
            childMocks,
            verificationMarks,
            exclusionRules
        )
    }

}

/**
 * Scope for static mockks. Part of DSL
 */
class MockKStaticScope(vararg val staticTypes: KClass<*>) : MockKUnmockKScope() {

    override fun doMock(): MockKCancellation {
        val factory = MockKGateway.implementation().staticMockFactory

        val cancellations = staticTypes.map {
            factory.staticMockk(it)
        }

        return { cancellations.forEach { it() } }
    }

    override fun clear(
        answers: Boolean,
        recordedCalls: Boolean,
        childMocks: Boolean,
        verificationMarks: Boolean,
        exclusionRules: Boolean
    ) {
        for (type in staticTypes) {
            MockKGateway.implementation().staticMockFactory.clear(
                type,
                MockKGateway.ClearOptions(
                    answers,
                    recordedCalls,
                    childMocks,
                    verificationMarks,
                    exclusionRules
                )
            )
        }
    }

    inline fun <reified T : Any> and() = MockKStaticScope(T::class, *staticTypes)

}

/**
 * Scope for object mockks. Part of DSL
 */
class MockKObjectScope(vararg val objects: Any, val recordPrivateCalls: Boolean = false) : MockKUnmockKScope() {
    override fun doMock(): MockKCancellation {

        val factory = MockKGateway.implementation().objectMockFactory

        val cancellations = objects.map {
            factory.objectMockk(it, recordPrivateCalls)
        }

        return { cancellations.forEach { it() } }
    }

    override fun clear(
        answers: Boolean,
        recordedCalls: Boolean,
        childMocks: Boolean,
        verificationMarks: Boolean,
        exclusionRules: Boolean
    ) {
        for (obj in objects) {
            MockKGateway.implementation().objectMockFactory.clear(
                obj,
                MockKGateway.ClearOptions(
                    answers,
                    recordedCalls,
                    childMocks,
                    verificationMarks,
                    exclusionRules
                )
            )
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun and(obj: Any) = MockKObjectScope(obj, *objects)
}

/**
 * Scope for constructor calls. Part of DSL.
 */
class MockKConstructorScope<T : Any>(
    val type: KClass<T>,
    val recordPrivateCalls: Boolean,
    val localToThread: Boolean
) : MockKUnmockKScope() {

    override fun doMock(): MockKCancellation {
        return MockKGateway.implementation().constructorMockFactory.constructorMockk(
            type, recordPrivateCalls, localToThread
        )
    }

    override fun clear(
        answers: Boolean,
        recordedCalls: Boolean,
        childMocks: Boolean,
        verificationMarks: Boolean,
        exclusionRules: Boolean
    ) {
        MockKGateway.implementation().constructorMockFactory.clear(
            type,
            MockKGateway.ClearOptions(
                answers,
                recordedCalls,
                childMocks,
                verificationMarks,
                exclusionRules
            )
        )
    }
}

/**
 * Wraps block of code for safe resource allocation and deallocation. Part of DSL
 */
inline fun <T> MockKUnmockKScope.use(block: () -> T): T {
    mock()
    return try {
        block()
    } finally {
        unmock()
    }
}

/**
 * Slot allows to capture one value.
 *
 * If this values is lambda then it's possible to invoke it.
 */
class CapturingSlot<T : Any>() {
    var isCaptured = false

    var isNull = false

    lateinit var captured: T

    fun clear() {
        isCaptured = false
        isNull = false
    }

    override fun toString(): String =
        "slot(${if (isCaptured) "captured=${if (isNull) "null" else captured.toStr()}" else ""})"
}

inline fun <reified T : () -> R, R> CapturingSlot<T>.invoke() = captured.invoke()
inline fun <reified T : (A1) -> R, R, A1> CapturingSlot<T>.invoke(arg1: A1) = captured.invoke(arg1)
inline fun <reified T : (A1, A2) -> R, R, A1, A2> CapturingSlot<T>.invoke(arg1: A1, arg2: A2) =
    captured.invoke(arg1, arg2)

inline fun <reified T : (A1, A2, A3) -> R, R, A1, A2, A3> CapturingSlot<T>.invoke(arg1: A1, arg2: A2, arg3: A3) =
    captured.invoke(arg1, arg2, arg3)

inline fun <reified T : (A1, A2, A3, A4) -> R, R, A1, A2, A3, A4> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4
) = captured.invoke(arg1, arg2, arg3, arg4)

inline fun <reified T : (A1, A2, A3, A4, A5) -> R, R, A1, A2, A3, A4, A5> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5
) = captured.invoke(arg1, arg2, arg3, arg4, arg5)

inline fun <reified T : (A1, A2, A3, A4, A5, A6) -> R, R, A1, A2, A3, A4, A5, A6> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7) -> R, R, A1, A2, A3, A4, A5, A6, A7> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15
) = captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16
)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16,
    arg17
)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16,
    arg17,
    arg18
)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16,
    arg17,
    arg18,
    arg19
)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19,
    arg20: A20
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16,
    arg17,
    arg18,
    arg19,
    arg20
)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19,
    arg20: A20,
    arg21: A21
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16,
    arg17,
    arg18,
    arg19,
    arg20,
    arg21
)

inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> CapturingSlot<T>.invoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19,
    arg20: A20,
    arg21: A21,
    arg22: A22
) = captured.invoke(
    arg1,
    arg2,
    arg3,
    arg4,
    arg5,
    arg6,
    arg7,
    arg8,
    arg9,
    arg10,
    arg11,
    arg12,
    arg13,
    arg14,
    arg15,
    arg16,
    arg17,
    arg18,
    arg19,
    arg20,
    arg21,
    arg22
)

inline fun <reified T : suspend () -> R, R> CapturingSlot<T>.coInvoke() =
    InternalPlatformDsl.runCoroutine { captured.invoke() }

inline fun <reified T : suspend (A1) -> R, R, A1> CapturingSlot<T>.coInvoke(arg1: A1) =
    InternalPlatformDsl.runCoroutine { captured.invoke(arg1) }

inline fun <reified T : suspend (A1, A2) -> R, R, A1, A2> CapturingSlot<T>.coInvoke(arg1: A1, arg2: A2) =
    InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2) }

inline fun <reified T : suspend (A1, A2, A3) -> R, R, A1, A2, A3> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3) }

inline fun <reified T : suspend (A1, A2, A3, A4) -> R, R, A1, A2, A3, A4> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5) -> R, R, A1, A2, A3, A4, A5> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4, arg5) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6) -> R, R, A1, A2, A3, A4, A5, A6> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7) -> R, R, A1, A2, A3, A4, A5, A6, A7> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10
) = InternalPlatformDsl.runCoroutine { captured.invoke(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10) }

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16,
        arg17
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16,
        arg17,
        arg18
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16,
        arg17,
        arg18,
        arg19
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19,
    arg20: A20
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16,
        arg17,
        arg18,
        arg19,
        arg20
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19,
    arg20: A20,
    arg21: A21
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16,
        arg17,
        arg18,
        arg19,
        arg20,
        arg21
    )
}

inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) -> R, R, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> CapturingSlot<T>.coInvoke(
    arg1: A1,
    arg2: A2,
    arg3: A3,
    arg4: A4,
    arg5: A5,
    arg6: A6,
    arg7: A7,
    arg8: A8,
    arg9: A9,
    arg10: A10,
    arg11: A11,
    arg12: A12,
    arg13: A13,
    arg14: A14,
    arg15: A15,
    arg16: A16,
    arg17: A17,
    arg18: A18,
    arg19: A19,
    arg20: A20,
    arg21: A21,
    arg22: A22
) = InternalPlatformDsl.runCoroutine {
    captured.invoke(
        arg1,
        arg2,
        arg3,
        arg4,
        arg5,
        arg6,
        arg7,
        arg8,
        arg9,
        arg10,
        arg11,
        arg12,
        arg13,
        arg14,
        arg15,
        arg16,
        arg17,
        arg18,
        arg19,
        arg20,
        arg21,
        arg22
    )
}


/**
 * Checks if argument is matching some criteria
 */
interface Matcher<in T> {
    fun match(arg: T?): Boolean

    fun substitute(map: Map<Any, Any>): Matcher<T> = this
}

/**
 * Checks if argument is of specific type
 */
interface TypedMatcher {
    val argumentType: KClass<*>

    fun checkType(arg: Any?): Boolean {
        return when {
            argumentType.simpleName === null -> true
            else -> {
                val unboxedClass = argumentType.boxedClass
                return unboxedClass.isInstance(arg)
            }
        }
    }
}

/**
 * Allows to substitute matcher to find correct chained call
 */
interface EquivalentMatcher {
    fun equivalent(): Matcher<Any>
}

/**
 * Captures the argument
 */
interface CapturingMatcher {
    fun capture(arg: Any?)
}

/**
 * Matcher composed from several other matchers.
 *
 * Allows to build matching expressions. Alike "and(eq(5), capture(lst))"
 */
interface CompositeMatcher<T> {
    val operandValues: List<T>

    var subMatchers: List<Matcher<T>>?

}

/**
 * Provides return value for mocked function
 */
interface Answer<out T> {
    fun answer(call: Call): T

    suspend fun coAnswer(call: Call): T = answer(call)
}

/**
 * Manipulable field value
 */
class BackingFieldValue(
    val name: String,
    val getter: () -> Any?,
    val setter: (Any?) -> Unit
)

typealias BackingFieldValueProvider = () -> BackingFieldValue?

/**
 * Call happened for stubbed mock
 */
data class Call(
    val retType: KClass<*>,
    val invocation: Invocation,
    val matcher: InvocationMatcher,
    val fieldValueProvider: BackingFieldValueProvider
)

/**
 * Provides information about method
 */
data class MethodDescription(
    val name: String,
    val returnType: KClass<*>,
    val returnTypeNullable: Boolean,
    val returnsUnit: Boolean,
    val returnsNothing: Boolean,
    val isSuspend: Boolean,
    val isFnCall: Boolean,
    val declaringClass: KClass<*>,
    val paramTypes: List<KClass<*>>,
    val varArgsArg: Int,
    val privateCall: Boolean
) {
    override fun toString() = "$name(${argsToStr()})"

    fun argsToStr() = paramTypes.map(this::argToStr).joinToString(", ")

    fun argToStr(argType: KClass<*>) = argType.simpleName

    fun isToString() = name == "toString" && paramTypes.isEmpty()
    fun isHashCode() = name == "hashCode" && paramTypes.isEmpty()
    fun isEquals() = name == "equals" && paramTypes.size == 1 && paramTypes[0] == Any::class

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodDescription) return false

        return when {
            name != other.name -> false
            returnType != other.returnType -> false
            declaringClass != other.declaringClass -> false
            paramTypes != other.paramTypes -> false
            else -> true
        }

    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + declaringClass.hashCode()
        result = 31 * result + paramTypes.hashCode()
        return result
    }

}

/**
 * Mock invocation
 */
data class Invocation(
    val self: Any,
    val stub: Any,
    val method: MethodDescription,
    val args: List<Any?>,
    val timestamp: Long,
    val callStack: () -> List<StackElement>,
    val originalCall: () -> Any?,
    val fieldValueProvider: BackingFieldValueProvider
) {

    fun substitute(map: Map<Any, Any>) = Invocation(
        self.internalSubstitute(map),
        stub,
        method.internalSubstitute(map),
        args.internalSubstitute(map),
        timestamp,
        callStack,
        originalCall,
        fieldValueProvider
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Invocation) return false

        return when {
            self !== other.self -> false
            method != other.method -> false
            args != other.args -> false
            else -> true
        }
    }

    override fun hashCode(): Int {
        var result = InternalPlatformDsl.identityHashCode(self)
        result = 31 * result + method.hashCode()
        result = 31 * result + args.hashCode()
        return result
    }

    override fun toString(): String =
        "$self.${method.name}(${args.joinToString(", ", transform = { it.toStr() })})"

}

/**
 * Element of stack trace.
 */
data class StackElement(
    val className: String,
    val fileName: String,
    val methodName: String,
    val line: Int,
    val nativeMethod: Boolean
)

/**
 * Checks if invocation is matching via number of matchers
 */
data class InvocationMatcher(
    val self: Any,
    val method: MethodDescription,
    val args: List<Matcher<Any>>,
    val allAny: Boolean
) {
    fun substitute(map: Map<Any, Any>) = InvocationMatcher(
        self.internalSubstitute(map),
        method.internalSubstitute(map),
        args.internalSubstitute(map),
        allAny
    )

    fun match(invocation: Invocation): Boolean {
        if (self !== invocation.self) {
            return false
        }
        if (method != invocation.method) {
            return false
        }
        if (allAny) {
            if (args.size < invocation.args.size) {
                return false
            }
        } else {
            if (args.size != invocation.args.size) {
                return false
            }
        }

        for (i in invocation.args.indices) {
            val matcher = args[i]
            val arg = invocation.args[i]

            if (matcher is TypedMatcher) {
                if (!matcher.checkType(arg)) {
                    return false
                }
            }

            if (!matcher.match(arg)) {
                return false
            }
        }

        return true
    }


    fun captureAnswer(invocation: Invocation) {
        for ((idx, argMatcher) in args.withIndex()) {
            if (argMatcher is CapturingMatcher) {
                argMatcher.capture(invocation.args[idx])
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InvocationMatcher) return false

        return when {
            self !== other.self -> false
            method != other.method -> false
            args != other.args -> false
            else -> true
        }

    }

    override fun hashCode(): Int {
        var result = InternalPlatformDsl.identityHashCode(self)
        result = 31 * result + method.hashCode()
        result = 31 * result + args.hashCode()
        return result
    }

    override fun toString(): String {
        return "$self.${method.name}(${args.joinToString(", ")}))"
    }


}

/**
 * Matched call
 */
data class RecordedCall(
    val retValue: Any?,
    val isRetValueMock: Boolean,
    val retType: KClass<*>,
    val matcher: InvocationMatcher,
    val selfChain: RecordedCall?,
    val argChains: List<Any>?
) {
    override fun toString(): String {
        return "RecordedCall(retValue=${retValue.toStr()}, retType=${retType.toStr()}, isRetValueMock=$isRetValueMock matcher=$matcher)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordedCall) return false

        return when {
            retValue !== other.retValue -> false
            isRetValueMock != other.isRetValueMock -> false
            retType != other.retType -> false
            matcher != other.matcher -> false
            selfChain != other.selfChain -> false
            argChains != other.argChains -> false
            else -> true
        }

    }

    override fun hashCode(): Int {
        var result = retValue?.let { InternalPlatformDsl.identityHashCode(it) } ?: 0
        result = 31 * result + isRetValueMock.hashCode()
        result = 31 * result + retType.hashCode()
        result = 31 * result + matcher.hashCode()
        result = 31 * result + (selfChain?.hashCode() ?: 0)
        result = 31 * result + (argChains?.hashCode() ?: 0)
        return result
    }
}

/**
 * Allows to deregister something was registered before
 */
interface Deregisterable {
    fun deregister()
}


inline fun <T : Deregisterable, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        try {
            this.deregister()
        } catch (closeException: Throwable) {
            // skip
        }
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> T.internalSubstitute(map: Map<Any, Any>): T {
    return (map[this as Any? ?: return null as T] ?: this) as T
}

fun <T : Any> List<T>.internalSubstitute(map: Map<Any, Any>) = this.map { it.internalSubstitute(map) }
