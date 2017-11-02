package io.mockk

import io.mockk.impl.MockKInstance
import io.mockk.impl.toStr
import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * All mocks are implementing this interface
 */
interface MockK

/**
 * Exception thrown by library
 */
class MockKException(message: String) : RuntimeException(message)

/**
 * Builds a new mock for specified class
 */
inline fun <reified T> mockk(): T = MockKGateway.LOCATOR().mockk(T::class.java)

/**
 * Builds a new spy for specified class. Copies fields from object if provided
 */
inline fun <reified T> spyk(objToCopy: T? = null): T = MockKGateway.LOCATOR().spyk(T::class.java, objToCopy)

/**
 * Creates new capturing slot
 */
inline fun <reified T> slot() = CapturingSlot<T>()

/**
 * Creates new lambda args
 */
fun args(vararg v: Any?) = LambdaArgs(*v)

/**
 * Starts a block of stubbing. Part of DSL.
 */
fun <T> every(stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T> = MockKGateway.LOCATOR().every(stubBlock, null)

/**
 * Starts a block of stubbing for coroutines. Part of DSL.
 */
fun <T> coEvery(stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T> = MockKGateway.LOCATOR().every(null, stubBlock)

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
 * Verifies calls happened in the past. Part of DSL
 */
fun <T> verify(ordering: Ordering = Ordering.UNORDERED,
               inverse: Boolean = false,
               atLeast: Int = 1,
               atMost: Int = Int.MAX_VALUE,
               exactly: Int = -1,
               verifyBlock: MockKVerificationScope.() -> T) {
    MockKGateway.LOCATOR().verify(
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
fun <T> coVerify(ordering: Ordering = Ordering.UNORDERED,
                 inverse: Boolean = false,
                 atLeast: Int = 1,
                 atMost: Int = Int.MAX_VALUE,
                 exactly: Int = -1,
                 verifyBlock: suspend MockKVerificationScope.() -> T) {
    MockKGateway.LOCATOR().verify(
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
fun <T> verifyOrder(inverse: Boolean = false,
                    verifyBlock: MockKVerificationScope.() -> T) {
    verify(Ordering.ORDERED, inverse, verifyBlock = verifyBlock)
}

/**
 * Shortcut for sequence calls verification
 */
fun <T> verifySequence(inverse: Boolean = false,
                       verifyBlock: MockKVerificationScope.() -> T) {
    verify(Ordering.SEQUENCE, inverse, verifyBlock = verifyBlock)
}

/**
 * Resets information associated with mock
 */
fun clearMocks(vararg mocks: Any, answers: Boolean = true, recordedCalls: Boolean = true, childMocks: Boolean = true) {
    for (mock in mocks) {
        if (mock is MockKInstance) {
            mock.___clear(answers, recordedCalls, childMocks)
        }
    }
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
open class MockKMatcherScope(@JvmSynthetic @PublishedApi internal val gw: MockKGateway,
                             val lambda: CapturingSlot<Function<*>>) {

    inline fun <reified T> match(matcher: Matcher<T>): T {
        return gw.callRecorder.matcher(matcher, T::class.java)
    }

    inline fun <reified T> match(noinline matcher: (T?) -> Boolean): T = match(FunctionMatcher(matcher))
    inline fun <reified T> eq(value: T): T = match(EqMatcher(value))
    inline fun <reified T> refEq(value: T): T = match(EqMatcher(value, ref = true))
    inline fun <reified T> any(): T = match(ConstantMatcher(true))
    inline fun <reified T> capture(lst: MutableList<T>): T = match(CaptureMatcher(lst))
    inline fun <reified T> capture(lst: CapturingSlot<T>): T = match(CapturingSlotMatcher(lst))
    inline fun <reified T> captureNullable(lst: MutableList<T?>): T = match(CaptureNullableMatcher(lst))
    inline fun <reified T : Comparable<T>> cmpEq(value: T): T = match(ComparingMatcher(value, 0))
    inline fun <reified T : Comparable<T>> more(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) 2 else 1))
    inline fun <reified T : Comparable<T>> less(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) -2 else -1))
    inline fun <reified T> and(left: T, right: T) = match(AndOrMatcher(true, left, right))
    inline fun <reified T> or(left: T, right: T) = match(AndOrMatcher(false, left, right))
    inline fun <reified T> not(value: T) = match(NotMatcher(value))
    inline fun <reified T> isNull(inverse: Boolean = false) = match(NullCheckMatcher<T>(inverse))
    inline fun <reified T, R : T> ofType(cls: Class<R>) = match(TypeMatcher<T>(cls))
    inline fun <reified T : Function<*>> invoke(args: LambdaArgs) = match<T> {
        args.invoke<Any?>(it as Function<*>)
        true
    }

    inline fun <reified T> allAny(): T = match(AllAnyMatcher(0))

    @Suppress("NOTHING_TO_INLINE")
    inline fun <R, T : Any> R.hint(cls: KClass<T>, n: Int = 1): R {
        MockKGateway.LOCATOR().callRecorder.childType(cls.java, n)
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
        val matcher = CapturingSlotMatcher(lambda as CapturingSlot<T>)
        return gw.callRecorder.matcher(matcher, cls.java as Class<T>)
    }
}

/**
 * Part of DSL. Additional operations for verification scope.
 */
class MockKVerificationScope(gw: MockKGateway,
                             lambda: CapturingSlot<Function<*>>) : MockKMatcherScope(gw, lambda) {
    inline fun <reified T> assert(msg: String? = null, noinline assertion: (T?) -> Boolean): T = match {
        if (!assertion(it)) {
            throw AssertionError("Verification matcher assertion failed" +
                    (if (msg == null) ": $msg" else ""))
        }
        true
    }

    inline fun <reified T> any(noinline captureBlock: (T?) -> Boolean): T = match {
        captureBlock(it)
        true
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
class MockKStubScope<T>(@JvmSynthetic @PublishedApi internal val gw: MockKGateway,
                        private val lambda: CapturingSlot<Function<*>>) {
    infix fun answers(answer: Answer<T?>) = gw.callRecorder.answer(answer)

    infix fun returns(returnValue: T?) = answers(ConstantAnswer(returnValue))

    infix fun returnsMany(values: List<T?>) = answers(ManyAnswersAnswer(values))

    fun returnsMany(vararg values: T?) = returnsMany(values.toList())

    infix fun throws(ex: Throwable) = answers(ThrowingAnswer(ex))

    infix fun answers(answer: MockKAnswerScope.(Call) -> T?) =
            answers(FunctionAnswer({ MockKAnswerScope(gw, lambda, it).answer(it) }))

    @Suppress("UNUSED_PARAMETER")
    infix fun just(runs: Runs) = returns(null)
}

/**
 * Scope for answering functions. Part of DSL
 */
class MockKAnswerScope(private val gw: MockKGateway,
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

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T> MutableList<T>.captured() = last()

    val nothing = null
}

/**
 * Slot allows to capture one value.
 *
 * If this values is lambda then it's possible to invoke it.
 */
data class CapturingSlot<T>(var captured: T? = null) {
    operator inline fun <reified R> invoke(vararg args: Any?): R? {
        return LambdaArgs(*args).invoke<R>(captured!! as Function<*>)
    }
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

}


/**
 * Checks if argument is matching some criteria
 */
interface Matcher<in T> {
    fun match(arg: T?): Boolean
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
 * Mock invocation
 */
data class Invocation(val self: MockK,
                      val method: Method,
                      val superMethod: Method?,
                      val args: List<Any?>,
                      val timestamp: Long = System.nanoTime()) {
    override fun toString(): String {
        return "Invocation(self=$self, method=${method.toStr()}, args=$args)"
    }

    fun withSelf(newSelf: MockK) = Invocation(newSelf, method, superMethod, args, timestamp)
}

/**
 * Checks if invocation is matching via number of matchers
 */
data class InvocationMatcher(val self: Matcher<Any>,
                             val method: Matcher<Method>,
                             val args: List<Matcher<Any>>) {
    fun match(invocation: Invocation): Boolean {
        if (!self.match(invocation.self)) {
            return false
        }
        if (!method.match(invocation.method)) {
            return false
        }
        if (args.size != invocation.args.size) {
            return false
        }

        for (i in 0 until args.size) {
            if (!args[i].match(invocation.args[i])) {
                return false
            }
        }

        return true
    }

    fun withSelf(newSelf: Matcher<Any>) = InvocationMatcher(newSelf, method, args)
}

/**
 * Matched invocation
 */
data class Call(val retType: Class<*>,
                val invocation: Invocation,
                val matcher: InvocationMatcher,
                val chained: Boolean) {
    fun withInvocationAndMatcher(newInvocation: Invocation, newMatcher: InvocationMatcher) =
            Call(retType, newInvocation, newMatcher, chained)
}
