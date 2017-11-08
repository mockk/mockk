package io.mockk

import kotlin.reflect.KClass

/**
 * All mocks are implementing this interface
 */
interface MockK

/**
 * Exception thrown by library
 */
class MockKException(message: String, ex: Throwable? = null) : Throwable(message, ex)

/**
 * DSL entry points.
 */
@Suppress("NOTHING_TO_INLINE")
object MockKDsl {
    /**
     * Builds a new mock for specified class
     */
    inline fun <reified T : Any> mockk(name: String? = null, vararg moreInterfaces: KClass<*>): T = MockKGateway.implementation().mockFactory.mockk(T::class, name, moreInterfaces)

    /**
     * Builds a new spy for specified class. Copies fields from object if provided
     */
    inline fun <reified T : Any> spyk(objToCopy: T? = null, name: String? = null, vararg moreInterfaces: KClass<*>): T = MockKGateway.implementation().mockFactory.spyk(T::class, objToCopy, name, moreInterfaces)

    /**
     * Creates new capturing slot
     */
    inline fun <reified T : Any> slot() = CapturingSlot<T>()

    /**
     * Creates new lambda args
     */
    fun args(vararg v: Any?) = LambdaArgs(*v)

    /**
     * Starts a block of stubbing. Part of DSL.
     */
    inline fun <T> every(noinline stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = MockKGateway.implementation().stubber.every(stubBlock, null)

    /**
     * Starts a block of stubbing for coroutines. Part of DSL.
     */
    inline fun <T> coEvery(noinline stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T> = MockKGateway.implementation().stubber.every(null, stubBlock)

    /**
     * Verifies calls happened in the past. Part of DSL
     */
    inline fun <T> verify(ordering: Ordering = Ordering.UNORDERED,
                          inverse: Boolean = false,
                          atLeast: Int = 1,
                          atMost: Int = Int.MAX_VALUE,
                          exactly: Int = -1,
                          noinline verifyBlock: MockKVerificationScope.() -> T) {

        if (exactly < -1) {
            throw MockKException("exactly should be positive")
        }
        if (exactly == -1 && atLeast < 0) {
            throw MockKException("atLeast should be positive")
        }
        if (exactly == -1 && atMost < 0) {
            throw MockKException("atMost should be positive")
        }
        if (atLeast > atMost) {
            throw MockKException("atLeast should less or equal atMost")
        }

        MockKGateway.implementation().verifier.verify(
                ordering,
                inverse,
                atLeast,
                atMost,
                exactly,
                verifyBlock,
                null)
    }

    /**
     * Verify for coroutines
     */
    inline fun <T> coVerify(ordering: Ordering = Ordering.UNORDERED,
                            inverse: Boolean = false,
                            atLeast: Int = 1,
                            atMost: Int = Int.MAX_VALUE,
                            exactly: Int = -1,
                            noinline verifyBlock: suspend MockKVerificationScope.() -> T) {
        MockKGateway.implementation().verifier.verify(
                ordering,
                inverse,
                atLeast,
                atMost,
                exactly,
                null,
                verifyBlock)
    }

    /**
     * Shortcut for ordered calls verification
     */
    inline fun <T> verifyOrder(inverse: Boolean = false,
                               noinline verifyBlock: MockKVerificationScope.() -> T) {
        verify(Ordering.ORDERED, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Shortcut for sequence calls verification
     */
    inline fun <T> verifySequence(inverse: Boolean = false,
                                  noinline verifyBlock: MockKVerificationScope.() -> T) {
        verify(Ordering.SEQUENCE, inverse, verifyBlock = verifyBlock)
    }

    /**
     * Resets information associated with mock
     */
    fun clearMocks(vararg mocks: Any, answers: Boolean = true, recordedCalls: Boolean = true, childMocks: Boolean = true) {
        MockKGateway.implementation().mockFactory.clear(
                mocks = mocks,
                answers = answers,
                recordedCalls = recordedCalls,
                childMocks = childMocks)
    }

    /**
     * Executes block of code with registering and unregistering instance factory.
     */
    inline fun <reified T: Any> withInstanceFactory(noinline instanceFactory: () -> T, block: () -> Unit) {
        MockKGateway.registerInstanceFactory(T::class, instanceFactory).use {
            block()
        }
    }
}

/**
 * Verification orderding
 */
enum class Ordering {
    /**
     * Order is not important. Calls just should happen
     */
    UNORDERED,
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
open class MockKMatcherScope(val gateway: MockKGateway,
                             val lambda: CapturingSlot<Function<*>>) {

    inline fun <reified T : Any> match(matcher: Matcher<T>): T {
        return gateway.callRecorder.matcher(matcher, T::class)
    }

    inline fun <reified T : Any> match(noinline matcher: (T) -> Boolean): T = matchNullable {
        if (it == null) {
            false
        } else {
            matcher(it)
        }
    }

    inline fun <reified T : Any> matchNullable(noinline matcher: (T?) -> Boolean): T = match(FunctionMatcher(matcher, T::class))
    inline fun <reified T : Any> eq(value: T, inverse: Boolean = false): T = match(EqMatcher(value, inverse = inverse))
    inline fun <reified T : Any> refEq(value: T, inverse: Boolean = false): T = match(EqMatcher(value, ref = true, inverse = inverse))
    inline fun <reified T : Any> any(): T = match(ConstantMatcher(true))
    inline fun <reified T : Any> capture(lst: MutableList<T>): T = match(CaptureMatcher(lst, T::class))
    inline fun <reified T : Any> capture(lst: CapturingSlot<T>): T = match(CapturingSlotMatcher(lst, T::class))
    inline fun <reified T : Any> captureNullable(lst: MutableList<T?>): T = match(CaptureNullableMatcher(lst, T::class))
    inline fun <reified T : Comparable<T>> cmpEq(value: T): T = match(ComparingMatcher(value, 0, T::class))
    inline fun <reified T : Comparable<T>> more(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) 2 else 1, T::class))
    inline fun <reified T : Comparable<T>> less(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) -2 else -1, T::class))
    inline fun <reified T : Any> and(left: T, right: T) = match(AndOrMatcher(true, left, right))
    inline fun <reified T : Any> or(left: T, right: T) = match(AndOrMatcher(false, left, right))
    inline fun <reified T : Any> not(value: T) = match(NotMatcher(value))
    inline fun <reified T : Any> isNull(inverse: Boolean = false) = match(NullCheckMatcher<T>(inverse))
    inline fun <reified T : Any, R : T> ofType(cls: KClass<R>) = match(OfTypeMatcher<T>(cls))

    inline fun <reified T : () -> Any> invoke() = match(InvokeMatcher<T> { it() })
    inline fun <reified T : (A1) -> Any, A1> invoke(arg1: A1) = match(InvokeMatcher<T> { it(arg1) })
    inline fun <reified T : (A1, A2) -> Any, A1, A2> invoke(arg1: A1, arg2: A2) = match(InvokeMatcher<T> { it(arg1, arg2) })
    inline fun <reified T : (A1, A2, A3) -> Any, A1, A2, A3> invoke(arg1: A1, arg2: A2, arg3: A3) = match(InvokeMatcher<T> { it(arg1, arg2, arg3) })
    inline fun <reified T : (A1, A2, A3, A4) -> Any, A1, A2, A3, A4> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4) })
    inline fun <reified T : (A1, A2, A3, A4, A5) -> Any, A1, A2, A3, A4, A5> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6) -> Any, A1, A2, A3, A4, A5, A6> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7) -> Any, A1, A2, A3, A4, A5, A6, A7> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8) -> Any, A1, A2, A3, A4, A5, A6, A7, A8> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19, arg20: A20) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19, arg20: A20, arg21: A21) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, arg21) })
    inline fun <reified T : (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> invoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19, arg20: A20, arg21: A21, arg22: A22) = match(InvokeMatcher<T> { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, arg21, arg22) })

    inline fun <reified T : suspend () -> Any> coInvoke() = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it() } })
    inline fun <reified T : suspend (A1) -> Any, A1> coInvoke(arg1: A1) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1) } })
    inline fun <reified T : suspend (A1, A2) -> Any, A1, A2> coInvoke(arg1: A1, arg2: A2) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2) } })
    inline fun <reified T : suspend (A1, A2, A3) -> Any, A1, A2, A3> coInvoke(arg1: A1, arg2: A2, arg3: A3) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3) } })
    inline fun <reified T : suspend (A1, A2, A3, A4) -> Any, A1, A2, A3, A4> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5) -> Any, A1, A2, A3, A4, A5> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6) -> Any, A1, A2, A3, A4, A5, A6> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7) -> Any, A1, A2, A3, A4, A5, A6, A7> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8) -> Any, A1, A2, A3, A4, A5, A6, A7, A8> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19, arg20: A20) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19, arg20: A20, arg21: A21) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, arg21) } })
    inline fun <reified T : suspend (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22) -> Any, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> coInvoke(arg1: A1, arg2: A2, arg3: A3, arg4: A4, arg5: A5, arg6: A6, arg7: A7, arg8: A8, arg9: A9, arg10: A10, arg11: A11, arg12: A12, arg13: A13, arg14: A14, arg15: A15, arg16: A16, arg17: A17, arg18: A18, arg19: A19, arg20: A20, arg21: A21, arg22: A22) = match(InvokeMatcher<T> { MockKGateway.implementation().runCoroutine { it(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11, arg12, arg13, arg14, arg15, arg16, arg17, arg18, arg19, arg20, arg21, arg22) } })

    inline fun <reified T : Any> allAny(): T = match(AllAnyMatcher())

    @Suppress("NOTHING_TO_INLINE")
    inline fun <R, T : Any> R.hint(cls: KClass<T>, n: Int = 1): R {
        MockKGateway.implementation().callRecorder.hintNextReturnType(cls, n)
        return this
    }

    /**
     * Captures lambda function. "cls" is one of
     *
     * Function0::class.java, Function1::class.java ... Function22::class.java
     *
     * classes
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Function<*>> captureLambda(cls: KClass<out Function<*>>): T {
        val matcher = CapturingSlotMatcher(lambda as CapturingSlot<T>, T::class)
        return gateway.callRecorder.matcher(matcher, cls as KClass<T>)
    }

    inline fun <reified T : Any> coMatch(noinline matcher: suspend (T) -> Boolean): T = match {
        MockKGateway.implementation().runCoroutine {
            matcher(it)
        }
    }

    inline fun <reified T : Any> coMatchNullable(noinline matcher: suspend (T?) -> Boolean): T = matchNullable {
        MockKGateway.implementation().runCoroutine {
            matcher(it)
        }
    }
}

/**
 * Part of DSL. Additional operations for verification scope.
 */
class MockKVerificationScope(gw: MockKGateway,
                             lambda: CapturingSlot<Function<*>>) : MockKMatcherScope(gw, lambda) {
    inline fun <reified T : Any> assert(msg: String? = null, noinline assertion: (T) -> Boolean): T = match(AssertMatcher({ assertion(it as T) }, msg, T::class))
    inline fun <reified T : Any> assertNullable(msg: String? = null, noinline assertion: (T?) -> Boolean): T = match(AssertMatcher(assertion, msg, T::class, nullable = true))
    inline fun <reified T : Any> run(noinline captureBlock: MockKAssertScope.(T) -> Unit): T = match {
        MockKAssertScope(it).captureBlock(it)
        true
    }

    inline fun <reified T : Any> runNullable(noinline captureBlock: MockKAssertScope.(T?) -> Unit): T = matchNullable {
        MockKAssertScope(it).captureBlock(it)
        true
    }

    inline fun <reified T : Any> coAssert(msg: String? = null, noinline assertion: suspend (T) -> Boolean): T = assert(msg) {
        MockKGateway.implementation().runCoroutine {
            assertion(it)
        }
    }

    inline fun <reified T : Any> coAssertNullable(msg: String? = null, noinline assertion: suspend (T?) -> Boolean): T = assertNullable(msg) {
        MockKGateway.implementation().runCoroutine {
            assertion(it)
        }
    }

    inline fun <reified T : Any> coRun(noinline captureBlock: suspend MockKAssertScope.(T) -> Unit): T = run {
        MockKGateway.implementation().runCoroutine {
            captureBlock(it)
        }
    }

    inline fun <reified T : Any> coRunNullable(noinline captureBlock: suspend MockKAssertScope.(T?) -> Unit): T = runNullable {
        MockKGateway.implementation().runCoroutine {
            captureBlock(it)
        }
    }
}

class MockKAssertScope(val actual: Any?) {
    fun assertEquals(expected: Any?) {
        if (!MockKGateway.implementation().instantiator.deepEquals(expected, actual)) {
            throw AssertionError(format(actual, expected))
        }
    }

    fun assertEquals(msg: String, expected: Any?) {
        if (!MockKGateway.implementation().instantiator.deepEquals(expected, actual)) {
            throw AssertionError(format(actual, expected, msg))
        }
    }

    private fun format(actual: Any?, expected: Any?, message: String? = null): String {
        val msgFormatted = if (message != null) "$message " else ""

        return "${msgFormatted}expected [$expected] but found [$actual]"
    }

}

/**
 * Part of DSL. Object to represent phrase "just Runs"
 */
object Runs

/**
 * Stub scope. Part of DSL
 *
 * Allows to specify function result
 */
class MockKStubScope<T>(val gateway: MockKGateway,
                        private val lambda: CapturingSlot<Function<*>>) {
    infix fun answers(answer: Answer<T?>) = gateway.callRecorder.answer(answer)

    infix fun returns(returnValue: T?) = answers(ConstantAnswer(returnValue))

    infix fun returnsMany(values: List<T?>) = answers(ManyAnswersAnswer(values))

    fun returnsMany(vararg values: T?) = returnsMany(values.toList())

    infix fun throws(ex: Throwable) = answers(ThrowingAnswer(ex))

    infix fun answers(answer: MockKAnswerScope.(Call) -> T?) =
            answers(FunctionAnswer({ MockKAnswerScope(gateway, lambda, it).answer(it) }))


    infix fun coAnswers(answer: suspend MockKAnswerScope.(Call) -> T?) = answers {
        MockKGateway.implementation().runCoroutine {
            answer(it)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    infix fun just(runs: Runs) = returns(null)
}

/**
 * Scope for answering functions. Part of DSL
 */
class MockKAnswerScope(val gateway: MockKGateway,
                       val lambda: CapturingSlot<Function<*>>,
                       val call: Call) {

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

    val nothing = null
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

    operator inline fun <reified R> invoke(vararg args: Any?): R {
        return invokeNullable<R>(*args) as R
    }

    inline fun <reified R> invokeNullable(vararg args: Any?): R? {
        return LambdaArgs(*args).invoke<R>(captured as Function<*>)
    }

    fun clear() {
        isCaptured = false
        isNull = false
    }

    override fun toString(): String = "slot(${if (isCaptured) "captured=${if (isNull) "null" else captured.toString()}" else ""})"
}

class LambdaArgs(vararg val args: Any?) {
    @Suppress("UNCHECKED_CAST")
    inline fun <reified R> invoke(function: Function<*>): R? {
        return when (args.size) {
            0 -> (function as Function0<R?>).invoke()
            1 -> (function as Function1<Any?, R?>).invoke(args[0])
            2 -> (function as Function2<Any?, Any?, R?>).invoke(args[0], args[1])
            3 -> (function as Function3<Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2])
            4 -> (function as Function4<Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3])
            5 -> (function as Function5<Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4])
            6 -> (function as Function6<Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5])
            7 -> (function as Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6])
            8 -> (function as Function8<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
            9 -> (function as Function9<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8])
            10 -> (function as Function10<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9])
            11 -> (function as Function11<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10])
            12 -> (function as Function12<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11])
            13 -> (function as Function13<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12])
            14 -> (function as Function14<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13])
            15 -> (function as Function15<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14])
            16 -> (function as Function16<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15])
            17 -> (function as Function17<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16])
            18 -> (function as Function18<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17])
            19 -> (function as Function19<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18])
            20 -> (function as Function20<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19])
            21 -> (function as Function21<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20])
            22 -> (function as Function22<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20], args[21])
            else -> throw MockKException("too much arguments")
        }
    }

    override fun toString(): String = args.toString()


}


/**
 * Checks if argument is matching some criteria
 */
interface Matcher<in T> {
    fun match(arg: T?): Boolean
}

/**
 * Checks if argument is of specific type
 */
interface TypedMatcher {
    val argumentType: KClass<*>

    fun checkType(arg: Any?): Boolean = argumentType.isInstance(arg)
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

    fun CompositeMatcher<*>.captureSubMatchers(arg: Any?) {
        subMatchers?.let {
            it.filterIsInstance<CapturingMatcher>()
                    .forEach { it.capture(arg) }
        }
    }
}

/**
 * Provides return value for mocked function
 */
interface Answer<out T> {
    fun answer(call: Call): T
}

/**
 * Provides inromation about method
 */
data class MethodDescription(val name: String,
                             val returnType: KClass<*>,
                             val declaringClass: KClass<*>,
                             val paramTypes: List<KClass<*>>,
                             val langDependentRef: Any) {
    override fun toString() = "$name(${argsToStr()})"

    fun argsToStr() = paramTypes.map(this::argToStr).joinToString(", ")

    fun argToStr(argType: KClass<*>) = argType.simpleName
}

/**
 * Mock invocation
 */
data class Invocation(val self: MockK,
                      val method: MethodDescription,
                      val superMethod: MethodDescription?,
                      val args: List<Any?>,
                      val timestamp: Long) {
    override fun toString(): String {
        return "Invocation(self=$self, method=$method, args=${argsToStr()})"
    }

    fun argsToStr() = args.map(this::argToStr).joinToString(", ")

    fun argToStr(arg: Any?) =
            if (arg == null) {
                "null"
            } else if (arg is Function<*>) {
                "lambda {}"
            } else {
                arg.toString()
            }
}

/**
 * Checks if invocation is matching via number of matchers
 */
data class InvocationMatcher(val self: Any,
                             val method: MethodDescription,
                             val args: List<Matcher<Any>>) {
    fun match(invocation: Invocation): Boolean {
        if (self !== invocation.self) {
            return false
        }
        if (method != invocation.method) {
            return false
        }
        if (args.size != invocation.args.size) {
            return false
        }

        for (i in 0 until args.size) {
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

}

/**
 * Matched invocation
 */
data class Call(val retType: KClass<*>,
                val invocation: Invocation,
                val matcher: InvocationMatcher,
                val chained: Boolean)

