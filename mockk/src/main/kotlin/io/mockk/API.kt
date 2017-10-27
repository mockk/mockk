package io.mockk

import io.mockk.impl.MockKInstance
import io.mockk.impl.toStr
import java.lang.reflect.Method

/**
 * All mocks are implementing this interface
 */
interface MockK

/**
 * Exception thrown by framework
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
 * Starts a block of stubbing. Part of DSL.
 */
fun <T> every(stubBlock: suspend MockKScope.() -> T): MockKStubScope<T> = MockKGateway.LOCATOR().every(stubBlock)

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
               verifyBlock: suspend MockKScope.() -> T) {
    MockKGateway.LOCATOR().verify(
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
fun <T> verifyOrder(inverse: Boolean = false,
                    verifyBlock: suspend MockKScope.() -> T) {
    verify(Ordering.ORDERED, inverse, verifyBlock = verifyBlock)
}

/**
 * Shortcut for sequence calls verification
 */
fun <T> verifySequence(inverse: Boolean = false,
                       verifyBlock: suspend MockKScope.() -> T) {
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
 * Stubbing/verification scope. Part of DSL.
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
class MockKScope(@JvmSynthetic @PublishedApi internal val gw: MockKGateway,
                 val lambda: CapturingSlot<Function<*>>) {

    inline fun <reified T> match(matcher: Matcher<T>): T {
        return gw.callRecorder.matcher(matcher, T::class.java)
    }

    inline fun <reified T> match(noinline matcher: (T?) -> Boolean): T = match(FunctionMatcher(matcher))
    inline fun <reified T> eq(value: T): T = match(EqMatcher(value))
    inline fun <reified T> refEq(value: T): T = match(EqMatcher(value, ref = true))
    inline fun <reified T> any(): T = match(ConstantMatcher(true))
    inline fun <reified T> capture(lst: MutableList<T>): T = match(CaptureMatcher(lst))
    inline fun <reified T> captureNullable(lst: MutableList<T?>): T = match(CaptureNullableMatcher(lst))
    inline fun <reified T : Comparable<T>> cmpEq(value: T): T = match(ComparingMatcher(value, 0))
    inline fun <reified T : Comparable<T>> more(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) 2 else 1))
    inline fun <reified T : Comparable<T>> less(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) -2 else -1))
    inline fun <reified T> and(left: T, right: T) = match(AndOrMatcher(true, left, right))
    inline fun <reified T> or(left: T, right: T) = match(AndOrMatcher(false, left, right))
    inline fun <reified T> not(value: T) = match(NotMatcher(value))
    inline fun <reified T> isNull(inverse: Boolean = false) = match(NullCheckMatcher<T>(inverse))
    inline fun <reified T, R : T> ofType(cls: Class<R>) = match(TypeMatcher<T>(cls))

    inline fun <reified T> allAny(): T = match(AllAnyMatcher(0))

    @Suppress("NOTHING_TO_INLINE")
    inline fun <R, T> R.childAs(cls: Class<T>, n: Int = 1): R {
        MockKGateway.LOCATOR().callRecorder.childType(cls, n)
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
    inline fun <reified T : Function<*>> captureLambda(cls: Class<out Function<*>>): T {
        val matcher = CapturingSlotMatcher(lambda as CapturingSlot<T>)
        return gw.callRecorder.matcher(matcher, cls as Class<T>)
    }
}

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
    @Suppress("UNCHECKED_CAST")
    operator inline fun <reified R> invoke(vararg args: Any?): R? {
        return when (args.size) {
            0 -> (captured as Function0<R?>).invoke()
            1 -> (captured as Function1<Any?, R?>).invoke(args[0])
            2 -> (captured as Function2<Any?, Any?, R?>).invoke(args[0], args[1])
            3 -> (captured as Function3<Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2])
            4 -> (captured as Function4<Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3])
            5 -> (captured as Function5<Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4])
            6 -> (captured as Function6<Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5])
            7 -> (captured as Function7<Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6])
            8 -> (captured as Function8<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
            9 -> (captured as Function9<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8])
            10 -> (captured as Function10<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9])
            11 -> (captured as Function11<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10])
            12 -> (captured as Function12<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11])
            13 -> (captured as Function13<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12])
            14 -> (captured as Function14<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13])
            15 -> (captured as Function15<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14])
            16 -> (captured as Function16<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15])
            17 -> (captured as Function17<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16])
            18 -> (captured as Function18<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17])
            19 -> (captured as Function19<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18])
            20 -> (captured as Function20<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19])
            21 -> (captured as Function21<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20])
            22 -> (captured as Function22<Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, Any?, R?>).invoke(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20], args[21])
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
