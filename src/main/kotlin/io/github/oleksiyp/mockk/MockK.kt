package io.github.oleksiyp.mockk

import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.Loader
import javassist.bytecode.AccessFlag
import javassist.bytecode.ClassFile
import javassist.util.proxy.MethodFilter
import javassist.util.proxy.MethodHandler
import javassist.util.proxy.ProxyFactory
import javassist.util.proxy.ProxyObject
import kotlinx.coroutines.experimental.runBlocking
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import org.slf4j.LoggerFactory
import sun.reflect.ReflectionFactory
import java.lang.AssertionError
import java.lang.Class
import java.lang.ClassNotFoundException
import java.lang.Object
import java.lang.System
import java.lang.System.identityHashCode
import java.lang.Thread
import java.lang.ThreadLocal
import java.lang.Void
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.Collections.synchronizedList
import java.util.Collections.synchronizedMap
import java.util.logging.Level
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Byte
import kotlin.Comparable
import kotlin.Double
import kotlin.Float
import kotlin.Function0
import kotlin.Function1
import kotlin.Function10
import kotlin.Function11
import kotlin.Function12
import kotlin.Function13
import kotlin.Function14
import kotlin.Function15
import kotlin.Function16
import kotlin.Function17
import kotlin.Function18
import kotlin.Function19
import kotlin.Function2
import kotlin.Function20
import kotlin.Function21
import kotlin.Function22
import kotlin.Function3
import kotlin.Function4
import kotlin.Function5
import kotlin.Function6
import kotlin.Function7
import kotlin.Function8
import kotlin.Function9
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.PublishedApi
import kotlin.RuntimeException
import kotlin.Short
import kotlin.String
import kotlin.Throwable
import kotlin.Unit
import kotlin.arrayOf
import kotlin.coroutines.experimental.Continuation
import kotlin.emptyArray
import kotlin.let
import kotlin.repeat
import kotlin.synchronized
import kotlin.with

// ---------------------------- USER FACING --------------------------------

/**
 * Runner to transforms classes early with junit {@link org.junit.runner.RunWith}
 */
class MockKJUnitRunner(cls: Class<*>) : Runner() {

    private val pool = TranslatingClassPool(MockKClassTranslator())
    private val loader = Loader(pool)

    init {
        loader.delegateLoadingOf("org.junit.runner.")
        Thread.currentThread().contextClassLoader = loader
    }

    private val parentRunner = ParentRunnerFinderDynamicFinder(cls) { loader.loadClass(it.name) }.runner

    override fun run(notifier: RunNotifier?) {
        parentRunner.run(notifier)
    }

    override fun getDescription(): Description = parentRunner.description
}

/**
 * All mocks are implementing this interface
 */
interface MockK {
    val spiedObj: Any
}

/**
 * Builds a new mock for specified class
 */
inline fun <reified T> mockk(): T = MockKGateway.mockk(T::class.java)

/**
 * Builds a new spy for specified class
 */
inline fun <reified T> spyk(obj: T): T = MockKGateway.spyk(T::class.java, obj)

/**
 * Creates new capturing slot
 */
inline fun <reified T> slot() = CapturingSlot<T>()

/**
 * Starts a block of stubbing. Part of DSL.
 */
fun <T> every(mockBlock: suspend MockKScope.() -> T): MockKStubScope<T> {
    val gw = MockKGateway.LOCATOR()
    val callRecorder = gw.callRecorder
    callRecorder.startStubbing()
    runBlocking {
        val n = MockKGateway.N_CALL_ROUNDS
        val scope = MockKScope(gw)
        repeat(n) {
            callRecorder.catchArgs(it, n)
            scope.mockBlock()
        }
        callRecorder.catchArgs(n, n)
    }
    return MockKStubScope(gw)
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
 * Verifies calls happened in the past. Part of DSL
 */
fun <T> verify(ordering: Ordering = Ordering.UNORDERED,
               inverse: Boolean = false,
               atLeast: Int = 1,
               atMost: Int = Int.MAX_VALUE,
               exactly: Int = -1,
               mockBlock: suspend MockKScope.() -> T) {

    if (ordering != Ordering.UNORDERED) {
        if (atLeast != 1 || atMost != Int.MAX_VALUE || exactly != -1) {
            throw MockKException("atLeast, atMost, exactly is only allowed in unordered verify block")
        }
    }

    val gw = MockKGateway.LOCATOR()
    val callRecorder = gw.callRecorder
    callRecorder.startVerification()
    runBlocking {
        val n = MockKGateway.N_CALL_ROUNDS
        val scope = MockKScope(gw)
        repeat(n) {
            callRecorder.catchArgs(it, n)
            scope.mockBlock()
        }
        callRecorder.catchArgs(n, n)
    }
    callRecorder.verify(ordering, inverse,
            if (exactly != -1) exactly else atLeast,
            if (exactly != -1) exactly else atMost)
}

/**
 * Shortcut for ordered calls verification
 */
fun <T> verifyOrder(inverse: Boolean = false,
                    mockBlock: suspend MockKScope.() -> T) {
    verify(Ordering.ORDERED, inverse, mockBlock = mockBlock)
}

/**
 * Shortcut for sequence calls verification
 */
fun <T> verifySequence(inverse: Boolean = false,
                       mockBlock: suspend MockKScope.() -> T) {
    verify(Ordering.SEQUENCE, inverse, mockBlock = mockBlock)
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
class MockKScope(@JvmSynthetic @PublishedApi internal val gw: MockKGateway) {
    inline fun <reified T> match(matcher: Matcher<T>): T {
        return MockKGateway.matcherInCall(gw, matcher)
    }

    inline fun <reified T> match(noinline matcher: (T) -> Boolean): T = match(FunctionMatcher(matcher))
    inline fun <reified T> eq(value: T): T = match(EqMatcher(value))
    inline fun <reified T> refEq(value: T): T = match(EqMatcher(value, ref = true))
    inline fun <reified T> any(): T = match(ConstantMatcher(true))
    inline fun <reified T> capture(lst: MutableList<T>): T = match(CaptureMatcher(lst))
    inline fun <reified T> capture(slot: CapturingSlot<T>): T = match(CapturingSlotMatcher(slot))
    inline fun <reified T : Comparable<T>> cmpEq(value: T): T = match(ComparingMatcher(value, 0))
    inline fun <reified T : Comparable<T>> more(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) 2 else 1))
    inline fun <reified T : Comparable<T>> less(value: T, andEquals: Boolean = false): T = match(ComparingMatcher(value, if (andEquals) -2 else -1))
    inline fun <reified T> and(left: T, right: T) = match(AndOrMatcher(true, left, right))
    inline fun <reified T> or(left: T, right: T) = match(AndOrMatcher(false, left, right))
    inline fun <reified T> not(value: T) = match(NotMatcher(value))
    inline fun <reified T> isNull(inverse: Boolean = false) = match(NullCheckMatcher<T>(inverse))
    inline fun <reified T, R : T> ofType(cls: Class<R>) = match(TypeMatcher<T>(cls))
}

/**
 * Stub scope. Part of DSL
 *
 * Allows to specify function result
 */
class MockKStubScope<T>(@JvmSynthetic @PublishedApi internal val gw: MockKGateway) {
    infix fun answers(answer: Answer<T?>) = gw.callRecorder.answer(answer)

    infix fun returns(returnValue: T?) = answers(ConstantAnswer(returnValue))

    infix fun returnsMany(values: List<T?>) = answers(ManyAnswersAnswer(values))

    fun returnsMany(vararg values: T?) = returnsMany(values.toList())

    infix fun throws(ex: Throwable) = answers(ThrowingAnswer(ex))

    infix fun answers(answer: MockKAnswerScope.(Call) -> T?) =
            answers(FunctionAnswer({ MockKAnswerScope(gw, it).answer(it) }))
}

/**
 * Scope for answering functions. Part of DSL
 */
class MockKAnswerScope(private val gw: MockKGateway,
                       val call: Call) {

    val invocation = call.invocation
    val matcher = call.matcher

    val self
        get() = invocation.self as MockK

    inline fun <reified T> spiedObj() = self.spiedObj as T

    val method
        get() = invocation.method

    val args
        get() = invocation.args

    val nArgs
        get() = invocation.args.size

    inline fun <reified T> firstArg() = invocation.args[0] as T
    inline fun <reified T> secondArg() = invocation.args[1] as T
    inline fun <reified T> thirdArg() = invocation.args[2] as T
    inline fun <reified T> lastArg() = invocation.args[invocation.args.size - 1] as T

    inline fun <T> MutableList<T>.captured() = last()
}

/**
 * Slot allow to capture one value.
 *
 * If this values is lambda then it's possible to invoke it.
 */
data class CapturingSlot<T>(var captured: T? = null) {
    inline fun <reified R> invoke(vararg args: Any?): R? {
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
    fun match(arg: T): Boolean
}

/**
 * Captures the argument
 */
interface CapturingMatcher {
    fun capture(arg: Any)
}

/**
 * Matcher composed from several other matchers.
 *
 * Allows to build matching expressions. Alike "and(eq(5), capture(lst))"
 */
interface CompositeMatcher<T> {
    val operandValues: List<T>

    var subMatchers: List<Matcher<T>>?

    fun CompositeMatcher<*>.captureSubMatchers(arg: Any) {
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
                      val args: List<Any>,
                      val timestamp: Long = System.nanoTime()) {
    override fun toString(): String {
        return "Invocation(self=$self, method=${MockKGateway.toString(method)}, args=$args)"
    }

    fun withSelf(newSelf: MockK) = Invocation(newSelf, method, args, timestamp)
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
data class Call(val invocation: Invocation, val matcher: InvocationMatcher, val chained: Boolean) {
    fun withInvocationAndMatcher(newInvocation : Invocation, newMatcher: InvocationMatcher) =
            Call(newInvocation, newMatcher, chained)
}

/**
 * Answer and invocation matcher
 */
data class InvocationAnswer(val matcher: InvocationMatcher, val answer: Answer<*>)


/**
 * Matcher that checks equality. By reference and by value (equals method)
 */
data class EqMatcher<T>(val value: T, val ref: Boolean = false) : Matcher<T> {
    override fun match(arg: T): Boolean =
            if (ref) {
                arg === value
            } else {
                if (arg is Any && arg.javaClass.isArray) {
                    if (value !is Any || value.javaClass != arg.javaClass) {
                        false
                    } else {
                        when (arg.javaClass) {
                            BooleanArray::class.java -> Arrays.equals(arg as IntArray, value as IntArray)
                            ByteArray::class.java -> Arrays.equals(arg as ByteArray, value as ByteArray)
                            ShortArray::class.java -> Arrays.equals(arg as ShortArray, value as ShortArray)
                            IntArray::class.java -> Arrays.equals(arg as IntArray, value as IntArray)
                            LongArray::class.java -> Arrays.equals(arg as LongArray, value as LongArray)
                            FloatArray::class.java -> Arrays.equals(arg as FloatArray, value as FloatArray)
                            DoubleArray::class.java -> Arrays.equals(arg as DoubleArray, value as DoubleArray)
                            else -> Arrays.equals(arg as Array<Any>, value as Array<Any>)
                        }
                    }
                } else {
                    arg == value
                }
            }

    override fun toString(): String =
            if (ref)
                "refEq(${MockKGateway.toString(value)})"
            else
                "eq(${MockKGateway.toString(value)})"
}

/**
 * Matcher that always returns one same value.
 */
data class ConstantMatcher<T>(val constValue: Boolean) : Matcher<T> {
    override fun match(arg: T): Boolean = constValue

    override fun toString(): String = if (constValue) "any()" else "none()"
}

/**
 * Delegating matching to lambda function
 */
data class FunctionMatcher<T>(val matchingFunc: (T) -> Boolean) : Matcher<T> {
    override fun match(arg: T): Boolean = matchingFunc(arg)

    override fun toString(): String = "matcher()"
}

/**
 * Matcher capturing all results to the list.
 */
data class CaptureMatcher<T>(val captureList: MutableList<T>) : Matcher<T>, CapturingMatcher {
    override fun capture(arg: Any) {
        captureList.add(arg as T)
    }

    override fun match(arg: T): Boolean = true

    override fun toString(): String = "capture()"
}


/**
 * Matcher capturing one last value to the CapturingSlot
 */
data class CapturingSlotMatcher<T>(val captureSlot: CapturingSlot<T>) : Matcher<T>, CapturingMatcher {
    override fun capture(arg: Any) {
        captureSlot.captured = arg as T
    }

    override fun match(arg: T): Boolean = true

    override fun toString(): String = "slotCapture()"
}

/**
 * Matcher comparing values
 */
data class ComparingMatcher<T : Comparable<T>>(val value: T, val cmpFunc: Int) : Matcher<T> {
    override fun match(arg: T): Boolean {
        val n = arg.compareTo(value)
        return when (cmpFunc) {
            2 -> n >= 0
            1 -> n > 0
            0 -> n == 0
            -1 -> n < 0
            -2 -> n <= 0
            else -> throw MockKException("bad comparing function")
        }
    }

    override fun toString(): String =
            when (cmpFunc) {
                -2 -> "lessAndEquals($value)"
                -1 -> "less($value)"
                0 -> "cmpEq($value)"
                1 -> "more($value)"
                2 -> "moreAndEquals($value)"
                else -> throw MockKException("bad comparing function")
            }
}

/**
 * Boolean logic "AND" and "OR" matcher composed of two other matchers
 */
data class AndOrMatcher<T>(val and: Boolean,
                           val first: T,
                           val second: T) : Matcher<T>, CompositeMatcher<T>, CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(first, second)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T): Boolean =
            if (and)
                subMatchers!![0].match(arg) && subMatchers!![1].match(arg)
            else
                subMatchers!![0].match(arg) || subMatchers!![1].match(arg)

    override fun capture(arg: Any) {
        captureSubMatchers(arg)
    }

    override fun toString(): String {
        val sm = subMatchers
        val op = if (and) "and" else "or"
        return if (sm != null)
            "$op(${sm[0]}, ${sm[1]})"
        else
            "$op()"
    }


}

/**
 * Boolean logic "NOT" matcher composed of one matcher
 */
data class NotMatcher<T>(val value: T) : Matcher<T>, CompositeMatcher<T>, CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(value)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T) =
            !subMatchers!![0].match(arg)

    override fun capture(arg: Any) {
        captureSubMatchers(arg)
    }

    override fun toString(): String {
        val sm = subMatchers
        return if (sm != null)
            "not(${sm[0]})"
        else
            "not()"
    }
}

/**
 * Checks if argument is null or non-null
 */
data class NullCheckMatcher<T>(val inverse: Boolean) : Matcher<T> {
    override fun match(arg: T) = if (inverse) arg != null else arg == null

    override fun toString(): String {
        return if (inverse)
            "isNull()"
        else
            "nonNullable()"
    }
}

/**
 * Checks matcher data type
 */
data class TypeMatcher<T>(val cls: Class<*>) : Matcher<T> {
    override fun match(arg: T) = cls.isInstance(arg)

    override fun toString() = "ofType(${cls.name})"
}

/**
 * Returns one constant reply
 */
data class ConstantAnswer<T>(val constantValue: T?) : Answer<T?> {
    override fun answer(call: Call) = constantValue

    override fun toString(): String = "const($constantValue)"
}

/**
 * Delegates reply to the lambda function
 */
data class FunctionAnswer<T>(val answerFunc: (Call) -> T?) : Answer<T?> {
    override fun answer(call: Call): T? = answerFunc(call)

    override fun toString(): String = "answer()"
}

/**
 * Returns many different replies, each time moving the next list element.
 * Stops at the end.
 */
data class ManyAnswersAnswer<T>(val answers: List<T?>) : Answer<T?> {
    private var n = 0

    override fun answer(call: Call): T? {
        val next = if (n == answers.size - 1) n else n++
        return answers[next]
    }

}

/**
 * Throws exception instead of function reply
 */
data class ThrowingAnswer(val ex: Throwable) : Answer<Nothing> {
    override fun answer(call: Call): Nothing {
        throw ex
    }

}

/**
 * Exception thrown by framework
 */
class MockKException(message: String) : RuntimeException(message)

// ---------------------------- PROGRAMMER FACING --------------------------------

/**
 * Mediates mocking implementation
 */
interface MockKGateway {
    val callRecorder: CallRecorder

    val instantiator: Instantiator

    fun verifier(ordering: Ordering): Verifier

    companion object {
        val defaultImpl : MockKGateway = MockKGatewayImpl()
        var LOCATOR: () -> MockKGateway = { defaultImpl }

        private val log = logger<MockKGateway>()

        private val NO_ARGS_TYPE = Class.forName("\$NoArgsConstructorParamType")

        fun <T> proxy(cls: Class<T>): Any? {
            val factory = ProxyFactory()

            log.debug { "Building proxy for $cls" }

            return if (cls.isInterface) {
                factory.interfaces = arrayOf(cls, MockKInstance::class.java)
                factory.create(emptyArray(), emptyArray())
            } else {
                factory.interfaces = arrayOf(MockKInstance::class.java)
                factory.superclass = cls
                factory.create(arrayOf(NO_ARGS_TYPE), arrayOf<Any?>(null))
            }
        }

        fun <T> mockk(cls: Class<T>): T {
            log.info { "Creating mockk for $cls" }
            val obj = proxy(cls)
            (obj as ProxyObject).handler = MockKInstanceProxyHandler(cls, obj)
            return cls.cast(obj)
        }

        fun <T> spyk(cls: Class<T>, spiedObj: T): T {
            log.info { "Creating spyk for $cls" }
            val obj = proxy(cls)
            (obj as ProxyObject).handler = SpyKInstanceProxyHandler(cls, obj, spiedObj)
            return cls.cast(obj)
        }

        fun anyValue(type: Class<*>,
                     block: () -> Any? = {
                         MockKGateway.LOCATOR().instantiator.instantiate(type)
                     }): Any? {
            return when (type) {
                Void.TYPE -> Unit
                Boolean::class.java -> false
                Byte::class.java -> 0.toByte()
                Short::class.java -> 0.toShort()
                Int::class.java -> 0
                Long::class.java -> 0L
                Float::class.java -> 0.0F
                Double::class.java -> 0.0
                String::class.java -> ""
                Object::class.java -> Object()
                BooleanArray::class.java -> BooleanArray(0)
                ByteArray::class.java -> ByteArray(0)
                ShortArray::class.java -> ShortArray(0)
                IntArray::class.java -> IntArray(0)
                LongArray::class.java -> LongArray(0)
                FloatArray::class.java -> FloatArray(0)
                DoubleArray::class.java -> DoubleArray(0)
                else -> {
                    if (type.isArray) {
                        arrayOfNulls<Any>(0)
                    } else {
                        block()
                    }
                }
            }
        }

        inline fun <reified T> matcherInCall(gw: MockKGateway, matcher: Matcher<T>): T {
            return gw.callRecorder.matcher(matcher, T::class.java)
        }

        fun toString(obj: Any?): String {
            if (obj == null)
                return "null"
            if (obj is Method)
                return obj.toStr()
            return obj.toString()
        }

        val N_CALL_ROUNDS: Int = 64
    }
}

/**
 * Backs DSL and build a list of calls
 */
interface CallRecorder {
    fun startStubbing()

    fun startVerification()

    fun catchArgs(round: Int, n: Int)

    fun <T> matcher(matcher: Matcher<*>, cls: Class<T>): T

    fun call(invocation: Invocation): Any?

    fun answer(answer: Answer<*>)

    fun verify(ordering: Ordering, inverse: Boolean, min: Int, max: Int)
}

/**
 * Verifier takes the list of calls and checks what invocations happened to the mocks
 */
interface Verifier {
    fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult
}

/**
 * Result of verfication
 */
data class VerificationResult(val matches: Boolean, val matcher: InvocationMatcher? = null)

/**
 * Instantiates empty object for provided class
 */
interface Instantiator {
    fun <T> instantiate(cls: Class<T>): T
}

// ---------------------------- IMPLEMENTATION --------------------------------

internal interface MockKInstance : MockK {
    fun ___type(): Class<*>

    fun ___addAnswer(matcher: InvocationMatcher, answer: Answer<*>)

    fun ___answer(invocation: Invocation): Any?

    fun ___childMockK(call: Call): MockKInstance

    fun ___recordCall(invocation: Invocation)

    fun ___matchesAnyRecordedCalls(matcher: InvocationMatcher, min: Int, max: Int): Boolean

    fun ___allRecordedCalls(): List<Invocation>

    fun ___clear(answers: Boolean, calls: Boolean, childMocks: Boolean)
}

private fun Method.toStr() =
        name + "(" + parameterTypes.map { it.simpleName }.joinToString() + ")"

private open class MockKInstanceProxyHandler(private val cls: Class<*>,
                                             private val obj: Any) : MethodHandler, MockKInstance {
    private val answers = synchronizedList(mutableListOf<InvocationAnswer>())
    private val childs = synchronizedMap(hashMapOf<InvocationMatcher, MockKInstance>())
    private val recordedCalls = synchronizedList(mutableListOf<Invocation>())

    override val spiedObj: Any
        get() = throw MockKException("spiedObj is actual only for spies")

    override fun ___addAnswer(matcher: InvocationMatcher, answer: Answer<*>) {
        answers.add(InvocationAnswer(matcher, answer))
    }

    override fun ___answer(invocation: Invocation): Any? {
        return synchronized(answers) {

            val invocationAndMatcher = answers
                    .reversed()
                    .firstOrNull { it.matcher.match(invocation) }

            invocationAndMatcher?.let {

                ___captureAnswer(it.matcher, invocation)

                val call = Call(invocation, it.matcher, false)
                it.answer.answer(call)
            } ?: defaultAnswer(invocation)
        }
    }

    private fun ___captureAnswer(invocationMatcher: InvocationMatcher, invocation: Invocation) {
        repeat(invocationMatcher.args.size) {
            val argMatcher = invocationMatcher.args[it]
            if (argMatcher is CapturingMatcher) {
                argMatcher.capture(invocation.args[it])
            }
        }
    }

    protected open fun defaultAnswer(invocation: Invocation): Any? {
        val cls = invocation.method.returnType
        return MockKGateway.anyValue(cls) { null }
    }

    override fun ___recordCall(invocation: Invocation) {
        recordedCalls.add(invocation)
    }

    override fun ___matchesAnyRecordedCalls(matcher: InvocationMatcher, min: Int, max: Int): Boolean {
        synchronized(recordedCalls) {
            val n = recordedCalls.filter { matcher.match(it) }.count()
            return n in min..max
        }
    }

    override fun ___allRecordedCalls(): List<Invocation> {
        synchronized(recordedCalls) {
            return recordedCalls.toList()
        }
    }

    override fun ___type(): Class<*> = cls

    override fun toString() = "mockk<" + ___type().simpleName + ">()"

    override fun equals(other: Any?): Boolean {
        return obj === other
    }

    override fun hashCode(): Int {
        return identityHashCode(obj)
    }

    override fun ___childMockK(call: Call): MockKInstance {
        return childs.computeIfAbsent(call.matcher, {
            MockKGateway.mockk(call.invocation.method.returnType) as MockKInstance
        })
    }

    override fun invoke(self: Any,
                        thisMethod: Method,
                        proceed: Method?,
                        args: Array<out Any>): Any? {

        findMethodInProxy(this, thisMethod)?.let {
            try {
                return it.invoke(this, *args)
            } catch (ex: InvocationTargetException) {
                throw ex.cause!!
            }
        }

        val argList = args.toList()
        val invocation = Invocation(self as MockKInstance, thisMethod, argList)
        return MockKGateway.LOCATOR().callRecorder.call(invocation)
    }

    private fun findMethodInProxy(obj: Any,
                                  method: Method): Method? {
        return obj.javaClass.methods.find {
            it.name == method.name &&
                    Arrays.equals(it.parameterTypes, method.parameterTypes)
        }
    }

    override fun ___clear(answers: Boolean, calls: Boolean, childMocks: Boolean) {
        if (answers) {
            this.answers.clear()
        }
        if (calls) {
            this.recordedCalls.clear()
        }
        if (childMocks) {
            this.childs.clear()
        }
    }
}


private class SpyKInstanceProxyHandler<T>(cls: Class<T>, obj: ProxyObject,
                                          private val _spiedObj: T) : MockKInstanceProxyHandler(cls, obj) {
    override val spiedObj
        get() = _spiedObj as Any

    override fun defaultAnswer(invocation: Invocation): Any? {
        return invocation.method.invoke(_spiedObj, *invocation.args.toTypedArray())
    }

    override fun toString(): String = "spyk<" + ___type().simpleName + ">()"
}


private class MockKGatewayImpl : MockKGateway {
    private val callRecorderTL = ThreadLocal.withInitial { CallRecorderImpl(this) }
    private val instantiatorTL = ThreadLocal.withInitial { InstantiatorImpl(this) }
    private val unorderedVerifierTL = ThreadLocal.withInitial { UnorderedVerifierImpl(this) }
    private val orderedVerifierTL = ThreadLocal.withInitial { OrderedVerifierImpl(this) }
    private val sequenceVerifierTL = ThreadLocal.withInitial { SequenceVerifierImpl(this) }

    override val callRecorder: CallRecorder
        get() = callRecorderTL.get()

    override val instantiator: Instantiator
        get() = instantiatorTL.get()

    override fun verifier(ordering: Ordering): Verifier =
            when (ordering) {
                Ordering.UNORDERED -> unorderedVerifierTL.get()
                Ordering.ORDERED -> orderedVerifierTL.get()
                Ordering.SEQUENCE -> sequenceVerifierTL.get()
            }
}

private class Ref(val value: Any) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Ref

        if (value !== other.value) return false

        return true
    }

    override fun hashCode(): Int = identityHashCode(value)
    override fun toString(): String = "Ref(${value.javaClass.simpleName}@${hashCode()})"
}

private data class SignedCall(val invocation: Invocation,
                              val matchers: List<Matcher<*>>,
                              val signaturePart: List<Any>)

private data class CallRound(val calls: List<SignedCall>)

private fun Invocation.self() = self as MockKInstance

private class CallRecorderImpl(private val gw: MockKGateway) : CallRecorder {
    private val log = logger<CallRecorderImpl>()

    private enum class Mode {
        STUBBING, VERIFYING, ANSWERING
    }

    private var mode = Mode.ANSWERING

    private val rnd = Random()

    private val signedCalls = mutableListOf<SignedCall>()
    private val callRounds = mutableListOf<CallRound>()
    private val calls = mutableListOf<Call>()
    private val childMocks = mutableListOf<Ref>()

    val matchers = mutableListOf<Matcher<*>>()
    val signatures = mutableListOf<Any>()

    fun checkMode(vararg modes: Mode) {
        if (!modes.any { it == mode }) {
            throw MockKException("Bad recording sequence. Mode: $mode")
        }
    }

    override fun startStubbing() {
        log.info { "Starting stubbing" }
        checkMode(Mode.ANSWERING)
        mode = Mode.STUBBING
        childMocks.clear()
    }

    override fun startVerification() {
        log.info { "Starting verification" }
        checkMode(Mode.ANSWERING)
        mode = Mode.VERIFYING
        childMocks.clear()
    }

    override fun catchArgs(round: Int, n: Int) {
        checkMode(Mode.STUBBING, Mode.VERIFYING)
        if (round > 0) {
            callRounds.add(CallRound(signedCalls.toList()))
            signedCalls.clear()
        }
        if (round == n) {
            signMatchers()
            mockRealChilds()
            callRounds.clear()
        }
    }

    private fun signMatchers() {
        val nCalls = callRounds[0].calls.size
        if (nCalls == 0) {
            throw MockKException("No calls inside every/verify {} block")
        }
        if (callRounds.any { it.calls.size != nCalls }) {
            throw MockKException("Not all call rounds result in same amount of calls")
        }

        calls.clear()

        repeat(nCalls) { callN ->

            val callInAllRounds = callRounds.map { it.calls[callN] }
            val matcherMap = hashMapOf<List<Any>, Matcher<*>>()
            val compositeMatchers = mutableListOf<List<CompositeMatcher<*>>>()
            val zeroCall = callInAllRounds[0]

            log.info { "Processing call #${callN}: ${zeroCall.invocation.method.toStr()}" }

            repeat(zeroCall.matchers.size) { nMatcher ->
                val matcher = callInAllRounds.map { it.matchers[nMatcher] }.last()
                val signature = callInAllRounds.map { it.signaturePart[nMatcher] }.toList()

                if (matcher is CompositeMatcher<*>) {
                    compositeMatchers.add(callInAllRounds.map {
                        it.matchers[nMatcher] as CompositeMatcher<*>
                    })
                }

                matcherMap[signature] = matcher
            }

            log.debug { "Matcher map for ${zeroCall.invocation.method.toStr()}: $matcherMap" }

            val argMatchers = mutableListOf<Matcher<*>>()

            repeat(zeroCall.invocation.args.size) { nArgument ->
                val signature = callInAllRounds.map {
                    val arg = it.invocation.args[nArgument]
                    if (passByValue(it.invocation.args[nArgument].javaClass))
                        arg
                    else
                        Ref(arg)
                }.toList()

                log.debug { "Signature for $nArgument argument of ${zeroCall.invocation.method.toStr()}: $signature" }

                val matcher = matcherMap.remove(signature)
                        ?: EqMatcher(zeroCall.invocation.args[nArgument])

                argMatchers.add(matcher)
            }

            for (cmList in compositeMatchers) {
                val matcher = cmList.last()

                matcher.subMatchers = matcher.operandValues.withIndex().map { (nOp, op) ->
                    val signature = cmList.map {
                        val arg = it.operandValues[nOp] as Any
                        if (passByValue(arg.javaClass))
                            arg
                        else
                            Ref(arg)
                    }.toList()

                    log.debug { "Signature for $nOp operand of $matcher composite matcher: $signature" }

                    matcherMap.remove(signature)
                            ?: EqMatcher(matcher.operandValues[nOp])
                } as List<Matcher<Any?>>?
            }

            if (zeroCall.invocation.method.isSuspend()) {
                log.debug { "Suspend function found. Replacing continuation with any() matcher" }
                argMatchers[argMatchers.size - 1] = ConstantMatcher<Any>(true)
            }

            if (matcherMap.isNotEmpty()) {
                throw MockKException("Failed to find few matchers by signature: $matcherMap")
            }

            val im = InvocationMatcher(
                    EqMatcher(zeroCall.invocation.self, ref = true),
                    EqMatcher(zeroCall.invocation.method),
                    argMatchers.toList() as List<Matcher<Any>>)
            log.info { "Built matcher: $im" }
            calls.add(Call(zeroCall.invocation, im,
                    childMocks.contains(Ref(zeroCall.invocation.self))))
        }
        childMocks.clear()
    }

    override fun <T> matcher(matcher: Matcher<*>, cls: Class<T>): T {
        checkMode(Mode.STUBBING, Mode.VERIFYING)
        matchers.add(matcher)
        val signatureValue = signatureValue(cls)
        signatures.add(if (passByValue(cls)) signatureValue as Any else Ref(signatureValue as Any))
        return signatureValue
    }


    private fun <T> signatureValue(cls: Class<T>): T {
        return cls.cast(when (cls) {
            java.lang.Boolean::class.java -> java.lang.Boolean(rnd.nextBoolean())
            java.lang.Byte::class.java -> java.lang.Byte(rnd.nextInt().toByte())
            java.lang.Short::class.java -> java.lang.Short(rnd.nextInt().toShort())
            java.lang.Character::class.java -> java.lang.Character(rnd.nextInt().toChar())
            java.lang.Integer::class.java -> java.lang.Integer(rnd.nextInt())
            java.lang.Long::class.java -> java.lang.Long(rnd.nextLong())
            java.lang.Float::class.java -> java.lang.Float(rnd.nextFloat())
            java.lang.Double::class.java -> java.lang.Double(rnd.nextDouble())
            java.lang.String::class.java -> java.lang.String(rnd.nextLong().toString(16))
            java.lang.Object::class.java -> java.lang.Object()
            else -> gw.instantiator.instantiate(cls)
        })
    }

    private fun passByValue(cls: Class<*>): Boolean {
        return when (cls) {
            java.lang.Boolean::class.java -> true
            java.lang.Byte::class.java -> true
            java.lang.Short::class.java -> true
            java.lang.Character::class.java -> true
            java.lang.Integer::class.java -> true
            java.lang.Long::class.java -> true
            java.lang.Float::class.java -> true
            java.lang.Double::class.java -> true
            java.lang.String::class.java -> true
            else -> false
        }
    }

    override fun call(invocation: Invocation): Any? {
        if (mode == Mode.ANSWERING) {
            invocation.self().___recordCall(invocation)
            val answer = invocation.self().___answer(invocation)
            log.info { "Recorded call: $invocation, answer: $answer" }
            return answer
        } else {
            return addCallWithMatchers(invocation)
        }
    }

    private fun addCallWithMatchers(invocation: Invocation): Any? {
        if (childMocks.any { mock -> invocation.args.any { it === mock } }) {
            throw MockKException("Passing child mocks to arguments is prohibited")
        }

        signedCalls.add(SignedCall(invocation, matchers.toList(), signatures.toList()))
        matchers.clear()
        signatures.clear()

        val cls = invocation.method.returnType
        return MockKGateway.anyValue(cls) {
            val child = MockKGateway.proxy(cls) as MockK
            (child as ProxyObject).handler = MockKInstanceProxyHandler(cls, child)
            childMocks.add(Ref(child))
            child
        }
    }

    fun mockRealChilds() {
        var newSelf: MockKInstance? = null
        val newCalls = mutableListOf<Call>()

        for ((idx, ic) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val invocation = ic.invocation

            if (!ic.chained) {
                newSelf = invocation.self()
            }

            val newInvocation = ic.invocation.withSelf(newSelf!!)
            val newMatcher = ic.matcher.withSelf(EqMatcher(newSelf, ref=true))
            val newCall = ic.withInvocationAndMatcher(newInvocation, newMatcher)

            newCalls.add(newCall)

            if (!lastCall && calls[idx + 1].chained) {
                newSelf = newSelf!!.___childMockK(newCall)
            }
        }

        calls.clear()
        calls.addAll(newCalls)

        log.debug { "Mocked childs" }
    }

    override fun answer(answer: Answer<*>) {
        checkMode(Mode.STUBBING)

        for ((idx, ic) in calls.withIndex()) {
            val lastCall = idx == calls.size - 1

            val ans = if (lastCall) {
                answer
            } else {
                ConstantAnswer(calls[idx + 1].invocation.self)
            }

            ic.invocation.self().___addAnswer(ic.matcher, ans)
        }

        calls.clear()

        log.debug { "Done stubbing" }
        mode = Mode.ANSWERING
    }

    override fun verify(ordering: Ordering, inverse: Boolean, min: Int, max: Int) {
        checkMode(Mode.VERIFYING)

        val outcome = gw.verifier(ordering).verify(calls, min, max)

        log.debug { "Done verification. Outcome: $outcome" }
        mode = Mode.ANSWERING

        failIfNotPassed(outcome, inverse)
    }

    private fun failIfNotPassed(outcome: VerificationResult, inverse: Boolean) {
        val matcherStr = if (outcome.matcher != null) ", matcher: ${outcome.matcher}" else ""

        if (inverse) {
            if (outcome.matches) {
                throw AssertionError("Inverse verification failed$matcherStr")
            }
        } else {
            if (!outcome.matches) {
                throw AssertionError("Verification failed$matcherStr")
            }
        }
    }
}

private class UnorderedVerifierImpl(private val gw: MockKGateway) : Verifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        return calls
                .firstOrNull { !it.invocation.self().___matchesAnyRecordedCalls(it.matcher, min, max) }
                ?.matcher
                ?.let { VerificationResult(false, it) }
                ?: VerificationResult(true)
    }
}

private fun List<Call>.allCalls() =
        this.map { Ref(it.invocation.self) }
                .distinct()
                .map { it.value as MockKInstance }
                .flatMap { it.___allRecordedCalls() }
                .sortedBy { it.timestamp }

private class OrderedVerifierImpl(private val gw: MockKGateway) : Verifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        val allCalls = calls.allCalls()

        if (calls.size > allCalls.size) {
            return VerificationResult(false)
        }

        // LCS algorithm
        var prev = Array(calls.size, { 0 })
        var curr = Array(calls.size, { 0 })
        for (call in allCalls) {
            for ((matcherIdx, matcher) in calls.map { it.matcher }.withIndex()) {
                curr[matcherIdx] = if (matcher.match(call)) {
                    if (matcherIdx == 0) 1 else prev[matcherIdx - 1] + 1
                } else {
                    maxOf(prev[matcherIdx], if (matcherIdx == 0) 0 else curr[matcherIdx - 1])
                }
            }
            val swap = curr
            curr = prev
            prev = swap
        }

        // match only if all matchers present
        return VerificationResult(prev[calls.size - 1] == calls.size)
    }
}

private class SequenceVerifierImpl(private val gw: MockKGateway) : Verifier {
    override fun verify(calls: List<Call>, min: Int, max: Int): VerificationResult {
        val allCalls = calls.allCalls()

        if (allCalls.size != calls.size) {
            return VerificationResult(false)
        }

        for ((i, call) in allCalls.withIndex()) {
            if (!calls[i].matcher.match(call)) {
                return VerificationResult(false)
            }
        }

        return VerificationResult(true)
    }
}


private fun Method.isSuspend(): Boolean {
    if (parameterCount == 0) {
        return false
    }
    return Continuation::class.java.isAssignableFrom(parameterTypes[parameterCount - 1])
}

// ---------------------------- BYTE CODE LEVEL --------------------------------

private class InstantiatorImpl(gw: MockKGatewayImpl) : Instantiator {
    private val log = logger<InstantiatorImpl>()

    val cp = ClassPool.getDefault()

    override fun <T> instantiate(cls: Class<T>): T {
        val factory = ProxyFactory()

        val makeMethod = factory.javaClass.getDeclaredMethod("make")
        makeMethod.isAccessible = true

        val computeSignatureMethod = factory.javaClass.getDeclaredMethod("computeSignature",
                MethodFilter::class.java)
        computeSignatureMethod.isAccessible = true

        val allocateClassNameMethod = factory.javaClass.getDeclaredMethod("allocateClassName")
        allocateClassNameMethod.isAccessible = true

        val proxyClsFile = if (cls.isInterface) {
            factory.interfaces = arrayOf(cls, MockKInstance::class.java)
            computeSignatureMethod.invoke(factory, MethodFilter { true })
            allocateClassNameMethod.invoke(factory)
            makeMethod.invoke(factory)
        } else {
            factory.interfaces = arrayOf(MockKInstance::class.java)
            factory.superclass = cls
            computeSignatureMethod.invoke(factory, MethodFilter { true })
            allocateClassNameMethod.invoke(factory)
            makeMethod.invoke(factory)
        } as ClassFile

        val proxyCls = cp.makeClass(proxyClsFile).toClass()

        val instance = newEmptyInstance(proxyCls)

        (instance as ProxyObject).handler = MethodHandler { self: Any, thisMethod: Method, proceed: Method, args: Array<Any?> ->

            if (thisMethod.name == "hashCode" && thisMethod.parameterCount == 0) {
                identityHashCode(self)
            } else if (thisMethod.name == "equals" &&
                    thisMethod.parameterCount == 1 &&
                    thisMethod.parameterTypes[0] == java.lang.Object::class.java) {
                self === args[0]
            } else {
                null
            }
        }

        log.debug { "Built instance $cls" }

        return cls.cast(instance)
    }

    val reflectionFactoryFinder =
            try {
                Class.forName("sun.reflect.ReflectionFactory")
                ReflecationFactoryFinder()
            } catch (cnf: ClassNotFoundException) {
                null
            }


    private fun newEmptyInstance(proxyCls: Class<*>): Any {
        reflectionFactoryFinder?.let { return it.newEmptyInstance(proxyCls) }
        throw MockKException("no instantiation support on platform")
    }
}

private class ReflecationFactoryFinder {
    fun newEmptyInstance(proxyCls: Class<*>): Any {
        val rf = ReflectionFactory.getReflectionFactory();
        val objDef = Object::class.java.getDeclaredConstructor();
        val intConstr = rf.newConstructorForSerialization(proxyCls, objDef)
        return intConstr.newInstance()
    }
}

internal class ParentRunnerFinder(val cls: Class<*>) {
    val parentRunner = findParentRunWith()

    fun findParentRunWith(): Runner {
        var parent = cls.superclass

        while (parent != null) {
            val annotation = parent.getAnnotation(RunWith::class.java)
            if (annotation != null) {
                val constructor = annotation.value.java.getConstructor(Class::class.java)
                return constructor.newInstance(cls)
            }
            parent = parent.superclass
        }
        throw RuntimeException("not runner RunWith found")
    }
}

private class ParentRunnerFinderDynamicFinder(cls: Class<*>, instrument: (Class<*>) -> Class<*>) {
    private val finderClass = instrument(ParentRunnerFinder::class.java)
    private val finderConstructor = finderClass.getConstructor(Class::class.java)
    private val getParentRunnerMethod = finderClass.getMethod("getParentRunner")
    val runner = getParentRunnerMethod.invoke(finderConstructor.newInstance(instrument(cls))) as Runner
}

private class TranslatingClassPool(private val mockKClassTranslator: MockKClassTranslator)
    : ClassPool() {

    init {
        appendSystemPath()
        mockKClassTranslator.start(this)
    }

    override fun get0(classname: String, useCache: Boolean): CtClass {
        val cls = super.get0(classname, useCache)
        mockKClassTranslator.onLoad(cls)
        return cls
    }
}

private class MockKClassTranslator {
    lateinit var noArgsParamType: CtClass
    val log = logger<MockKClassTranslator>()

    fun start(pool: ClassPool) {
        noArgsParamType = pool.makeClass("\$NoArgsConstructorParamType")
    }

    val load = Collections.synchronizedSet(hashSetOf<String>())

    fun onLoad(cls: CtClass) {
        if (!load.add(cls.name) || cls.isFrozen) {
            return
        }
        log.debug { "Translating ${cls.name}" }
        removeFinal(cls)
        addNoArgsConstructor(cls)
        cls.freeze()
    }

    private fun addNoArgsConstructor(cls: CtClass) {
        if (cls.isAnnotation || cls.isArray || cls.isEnum || cls.isInterface) {
            return
        }

        if (cls.constructors.any { isNoArgsConstructor(it) }) {
            return
        }

        if (cls.superclass == null) {
            return
        }

        with(cls.superclass) {
            when {
                constructors.any { isNoArgsConstructor(it) } -> {
                    if (cls.constructors.any { isNoArgsConstructor(it) }) {
                        return@with
                    }

                    val newConstructor = CtConstructor(arrayOf(noArgsParamType), cls)
                    cls.addConstructor(newConstructor)
                    newConstructor.setBody("super($1);")
                }
                constructors.any { it.parameterTypes.isEmpty() } -> {
                    if (cls.constructors.any { isNoArgsConstructor(it) }) {
                        return@with
                    }

                    val newConstructor = CtConstructor(arrayOf(noArgsParamType), cls)
                    cls.addConstructor(newConstructor)
                    newConstructor.setBody("super();")
                }
            }
        }
    }

    private fun isNoArgsConstructor(it: CtConstructor) =
            it.parameterTypes.size == 1 && it.parameterTypes[0] == noArgsParamType

    fun removeFinal(clazz: CtClass) {
        removeFinalOnClass(clazz)
        removeFinalOnMethods(clazz)
        clazz.stopPruning(true)
    }

    private fun removeFinalOnMethods(clazz: CtClass) {
        clazz.declaredMethods.forEach {
            if (java.lang.reflect.Modifier.isFinal(it.modifiers)) {
                it.modifiers = javassist.Modifier.clear(it.modifiers, java.lang.reflect.Modifier.FINAL)
            }
        }
    }


    private fun removeFinalOnClass(clazz: CtClass) {
        val modifiers = clazz.modifiers
        if (java.lang.reflect.Modifier.isFinal(modifiers)) {
            clazz.classFile2.accessFlags = AccessFlag.of(javassist.Modifier.clear(modifiers, java.lang.reflect.Modifier.FINAL))
        }
    }

}


// ---------------------------- LOGGING --------------------------------

private val loggerFactory = try {
    Class.forName("org.slf4j.Logger");
    { cls: Class<*> -> Slf4jLogger(cls) }
} catch (ex: ClassNotFoundException) {
    { cls: Class<*> -> JULLogger(cls) }
}


private inline fun <reified T> logger(): Logger = loggerFactory(T::class.java)

private interface Logger {
    fun error(msg: () -> String)
    fun error(ex: Throwable, msg: () -> String)
    fun warn(msg: () -> String)
    fun warn(ex: Throwable, msg: () -> String)
    fun info(msg: () -> String)
    fun info(ex: Throwable, msg: () -> String)
    fun debug(msg: () -> String)
    fun debug(ex: Throwable, msg: () -> String)
}

private class Slf4jLogger(cls: Class<*>) : Logger {
    val log = LoggerFactory.getLogger(cls)

    override fun error(msg: () -> String) = if (log.isErrorEnabled) log.error(msg()) else Unit
    override fun error(ex: Throwable, msg: () -> String) = if (log.isErrorEnabled) log.error(msg(), ex) else Unit
    override fun warn(msg: () -> String) = if (log.isWarnEnabled) log.warn(msg()) else Unit
    override fun warn(ex: Throwable, msg: () -> String) = if (log.isWarnEnabled) log.warn(msg(), ex) else Unit
    // note library info & debug is shifted to debug & trace respectively
    override fun info(msg: () -> String) = if (log.isDebugEnabled) log.debug(msg()) else Unit

    override fun info(ex: Throwable, msg: () -> String) = if (log.isDebugEnabled) log.debug(msg(), ex) else Unit
    override fun debug(msg: () -> String) = if (log.isTraceEnabled) log.trace(msg()) else Unit
    override fun debug(ex: Throwable, msg: () -> String) = if (log.isTraceEnabled) log.trace(msg(), ex) else Unit
}

private class JULLogger(cls: Class<*>) : Logger {
    val log = java.util.logging.Logger.getLogger(cls.name)

    override fun error(msg: () -> String) = if (log.isLoggable(Level.SEVERE)) log.severe(msg()) else Unit
    override fun error(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.SEVERE)) log.log(Level.SEVERE, msg(), ex) else Unit
    override fun warn(msg: () -> String) = if (log.isLoggable(Level.WARNING)) log.warning(msg()) else Unit
    override fun warn(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.WARNING)) log.log(Level.WARNING, msg(), ex) else Unit
    // note library info & debug is shifted to debug & trace respectively
    override fun info(msg: () -> String) = if (log.isLoggable(Level.FINE)) log.fine(msg()) else Unit

    override fun info(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.FINE)) log.log(Level.FINE, msg(), ex) else Unit
    override fun debug(msg: () -> String) = if (log.isLoggable(Level.FINER)) log.finer(msg()) else Unit
    override fun debug(ex: Throwable, msg: () -> String) = if (log.isLoggable(Level.FINER)) log.log(Level.FINER, msg(), ex) else Unit
}
