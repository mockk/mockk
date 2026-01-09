package io.mockk

import io.mockk.InternalPlatformDsl.toArray
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.core.ValueClassSupport.boxedClass
import io.mockk.core.ValueClassSupport.boxedValue
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * Matcher that checks equality. By reference and by value (equals method)
 */
data class EqMatcher<in T : Any>(
    private val valueArg: T,
    val ref: Boolean = false,
    val inverse: Boolean = false,
) : Matcher<T> {
    val value = InternalPlatformDsl.unboxChar(valueArg).boxedValue

    override fun match(arg: T?): Boolean {
        val result =
            if (ref) {
                arg?.boxedValue === value
            } else {
                if (arg == null) {
                    false
                } else {
                    val unboxedArg = InternalPlatformDsl.unboxChar(arg).boxedValue
                    InternalPlatformDsl.deepEquals(unboxedArg, value)
                }
            }
        return if (inverse) !result else result
    }

    override fun substitute(map: Map<Any, Any>) = copy(valueArg = valueArg.internalSubstitute(map))

    override fun toString(): String =
        if (ref) {
            "${if (inverse) "refNonEq" else "refEq"}(${value.toStr()})"
        } else {
            "${if (inverse) "nonEq" else "eq"}(${value.toStr()})"
        }
}

/**
 * Matcher that always returns one same value.
 */
data class ConstantMatcher<in T : Any>(
    val constValue: Boolean,
) : Matcher<T> {
    override fun match(arg: T?): Boolean = constValue

    override fun toString(): String = if (constValue) "any()" else "none()"
}

/**
 * Delegates matching to lambda function
 */
data class FunctionMatcher<in T : Any>(
    val matchingFunc: (T) -> Boolean,
    override val argumentType: KClass<*>,
    private val assertionErrorLogger: (AssertionError) -> Unit = { e -> e.printStackTrace() },
) : Matcher<T>,
    TypedMatcher,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    override fun match(arg: T?): Boolean =
        if (arg == null) {
            false
        } else {
            try {
                matchingFunc(arg)
            } catch (a: AssertionError) {
                assertionErrorLogger(a)
                false
            }
        }

    override fun toString(): String = "matcher<${argumentType.simpleName}>()"
}

data class FunctionWithNullableArgMatcher<in T : Any>(
    val matchingFunc: (T?) -> Boolean,
    override val argumentType: KClass<*>,
    private val assertionErrorLogger: (AssertionError) -> Unit = { e -> e.printStackTrace() },
) : Matcher<T>,
    TypedMatcher,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    override fun match(arg: T?): Boolean =
        try {
            matchingFunc(arg)
        } catch (a: AssertionError) {
            assertionErrorLogger(a)
            false
        }

    override fun checkType(arg: Any?): Boolean {
        if (arg == null) {
            return true
        }

        return super.checkType(arg)
    }

    override fun toString(): String = "matcher<${argumentType.simpleName}>()"
}

/**
 * Matcher capturing all results to the list.
 */
data class CaptureMatcher<T : Any>(
    val captureList: MutableList<T>,
    override val argumentType: KClass<*>,
) : Matcher<T>,
    CapturingMatcher,
    TypedMatcher,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    override fun capture(arg: Any?) {
        if (arg != null) {
            captureList.add(InternalPlatformDsl.boxCast(argumentType, arg))
        }
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "capture<${argumentType.simpleName}>()"
}

/**
 * Matcher capturing all results to the list. Allows nulls
 */
data class CaptureNullableMatcher<T : Any>(
    val captureList: MutableList<T?>,
    override val argumentType: KClass<*>,
) : Matcher<T>,
    CapturingMatcher,
    TypedMatcher,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    @Suppress("UNCHECKED_CAST")
    override fun capture(arg: Any?) {
        captureList.add(arg as T?)
    }

    override fun match(arg: T?): Boolean = true

    override fun checkType(arg: Any?): Boolean {
        if (arg == null) {
            return true
        }

        return super.checkType(arg)
    }

    override fun toString(): String = "capture<${argumentType.simpleName}?>()"
}

/**
 * Matcher capturing one last NON nullable value to the [CapturingSlot]
 */
data class CapturingSlotMatcher<T : Any>(
    val captureSlot: CapturingSlot<T>,
    override val argumentType: KClass<*>,
) : Matcher<T>,
    CapturingMatcher,
    TypedMatcher,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    override fun capture(arg: Any?) {
        // does not capture null values
        if (arg != null) {
            captureSlot.captured = InternalPlatformDsl.boxCast(argumentType, arg)
        }
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "slotCapture<${argumentType.simpleName}>()"
}

/**
 * Matcher capturing one last nullable value to the [CapturingSlot]
 */
data class CapturingNullableSlotMatcher<T : Any>(
    val captureSlot: CapturingSlot<T?>,
    override val argumentType: KClass<*>,
) : Matcher<T>,
    CapturingMatcher,
    TypedMatcher,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    override fun capture(arg: Any?) {
        if (arg == null) {
            captureSlot.captured = null
        } else {
            captureSlot.captured = InternalPlatformDsl.boxCast(argumentType, arg)
        }
    }

    override fun match(arg: T?): Boolean = true

    override fun checkType(arg: Any?): Boolean {
        if (arg == null) {
            return true
        }

        return super.checkType(arg)
    }

    override fun toString(): String = "slotCapture<${argumentType.simpleName}>()"
}

/**
 * Matcher comparing values
 */
data class ComparingMatcher<T : Comparable<T>>(
    val value: T,
    val cmpFunc: Int,
    override val argumentType: KClass<T>,
) : Matcher<T>,
    TypedMatcher {
    override fun match(arg: T?): Boolean {
        if (arg == null) return false
        val n = arg.compareTo(value)
        return when (cmpFunc) {
            2 -> n >= 0
            1 -> n > 0
            0 -> n == 0
            -1 -> n < 0
            -2 -> n <= 0
            else -> throw MockKException("Bad comparison function")
        }
    }

    override fun substitute(map: Map<Any, Any>) = copy(value = value.internalSubstitute(map))

    override fun toString(): String =
        when (cmpFunc) {
            -2 -> "lessAndEquals($value)"
            -1 -> "less($value)"
            0 -> "cmpEq($value)"
            1 -> "more($value)"
            2 -> "moreAndEquals($value)"
            else -> throw MockKException("Bad comparison function")
        }
}

/**
 * Boolean logic "AND" and "OR" matcher composed of two other matchers
 */
data class AndOrMatcher<T : Any>(
    val and: Boolean,
    val first: T,
    val second: T,
) : Matcher<T>,
    CompositeMatcher<T>,
    CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(first, second)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T?): Boolean =
        if (and) {
            subMatchers!![0].match(arg) && subMatchers!![1].match(arg)
        } else {
            subMatchers!![0].match(arg) || subMatchers!![1].match(arg)
        }

    override fun substitute(map: Map<Any, Any>): Matcher<T> {
        val matcher =
            copy(
                first = first.internalSubstitute(map),
                second = second.internalSubstitute(map),
            )
        val sm = subMatchers
        if (sm != null) {
            matcher.subMatchers = sm.map { it.substitute(map) }
        }
        return matcher
    }

    override fun capture(arg: Any?) {
        captureSubMatchers(arg)
    }

    override fun toString(): String {
        val sm = subMatchers
        val op = if (and) "and" else "or"
        return if (sm != null) {
            "$op(${sm[0]}, ${sm[1]})"
        } else {
            "$op()"
        }
    }
}

/**
 * Boolean logic "NOT" matcher composed of one matcher
 */
data class NotMatcher<T : Any>(
    val value: T,
) : Matcher<T>,
    CompositeMatcher<T>,
    CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(value)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T?): Boolean = !subMatchers!![0].match(arg)

    override fun substitute(map: Map<Any, Any>): Matcher<T> {
        val matcher = copy(value = value.internalSubstitute(map))
        val sm = subMatchers
        if (sm != null) {
            matcher.subMatchers = sm.map { it.substitute(map) }
        }
        return matcher
    }

    override fun capture(arg: Any?) {
        captureSubMatchers(arg)
    }

    override fun toString(): String {
        val sm = subMatchers
        return if (sm != null) {
            "not(${sm[0]})"
        } else {
            "not()"
        }
    }
}

/**
 * Checks if argument is null or non-null
 */
data class NullCheckMatcher<in T : Any>(
    val inverse: Boolean = false,
) : Matcher<T> {
    override fun match(arg: T?): Boolean = if (inverse) arg != null else arg == null

    override fun toString(): String =
        if (inverse) {
            "notNull()"
        } else {
            "null()"
        }
}

/**
 * Checks matcher data type
 */
data class OfTypeMatcher<in T : Any>(
    val cls: KClass<*>,
) : Matcher<T> {
    override fun match(arg: T?): Boolean = cls.isInstance(arg)

    override fun toString() = "ofType(${cls.simpleName})"
}

/**
 * Matcher to replace all unspecified argument matchers to any()
 * Handled by logic in a special way
 */
class AllAnyMatcher<T> : Matcher<T> {
    override fun match(arg: T?): Boolean = true

    override fun toString() = "allAny()"
}

/**
 * Invokes lambda
 */
class InvokeMatcher<in T : Any>(
    val block: (T) -> Unit,
) : Matcher<T>,
    EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher(true)

    override fun match(arg: T?): Boolean {
        if (arg == null) {
            return true
        }
        block(arg)
        return true
    }

    override fun toString(): String = "coInvoke()"
}

/**
 * Matcher that can match arrays via provided matchers for each element.
 */
data class ArrayMatcher<in T : Any>(
    private val matchers: List<Matcher<Any>>,
) : Matcher<T>,
    CapturingMatcher {
    override fun capture(arg: Any?) {
        if (arg == null) {
            return
        }

        val arr = arg.toArray()

        repeat(min(arr.size, matchers.size)) { i ->
            val matcher = matchers[i]
            if (matcher is CapturingMatcher) {
                matcher.capture(arr[i])
            }
        }
    }

    override fun match(arg: T?): Boolean {
        if (arg == null) {
            return false
        }

        val arr = arg.toArray()

        if (arr.size != matchers.size) {
            return false
        }

        repeat(arr.size) { i ->
            if (!matchers[i].match(arr[i])) {
                return false
            }
        }

        return true
    }

    override fun substitute(map: Map<Any, Any>) = copy(matchers = matchers.map { it.substitute(map) })

    override fun toString(): String = matchers.joinToString(prefix = "[", postfix = "]")
}

data class VarargMatcher<T : Any>(
    private val all: Boolean,
    private val matcher: MockKMatcherScope.MockKVarargScope.(T?) -> Boolean,
    private val prefix: List<Matcher<T>> = listOf(),
    private val postfix: List<Matcher<T>> = listOf(),
) : Matcher<Any>,
    CapturingMatcher {
    @Suppress("UNCHECKED_CAST")
    override fun match(arg: Any?): Boolean {
        if (arg == null) {
            return false
        }

        val arr = arg.toArray()

        if (arr.size < prefix.size + postfix.size) {
            return false
        }

        repeat(prefix.size) {
            val el = arr[it] as T?
            if (!prefix[it].match(el)) {
                return false
            }
        }

        repeat(postfix.size) {
            val el = arr[arr.size - postfix.size + it] as T?
            if (!postfix[it].match(el)) {
                return false
            }
        }

        val centralPartSize = arr.size - postfix.size - prefix.size

        if (all) {
            repeat(centralPartSize) {
                val position = it + prefix.size
                val el = arr[position] as T?
                val scope = MockKMatcherScope.MockKVarargScope(position, arr.size)
                if (!scope.matcher(el)) {
                    return false
                }
            }
            return true
        } else {
            repeat(centralPartSize) {
                val position = it + prefix.size
                val el = arr[position] as T?
                val scope = MockKMatcherScope.MockKVarargScope(position, arr.size)
                if (scope.matcher(el)) {
                    return true
                }
            }
            return false
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun capture(arg: Any?) {
        if (arg == null) {
            return
        }

        val arr = arg.toArray()

        if (arr.size < prefix.size + postfix.size) {
            return
        }

        repeat(prefix.size) {
            val el = arr[it] as T?
            val elMatcher = prefix[it]
            if (elMatcher is CapturingMatcher) {
                elMatcher.capture(el)
            }
        }

        repeat(postfix.size) {
            val el = arr[arr.size - postfix.size + it] as T?
            val elMatcher = postfix[it]
            if (elMatcher is CapturingMatcher) {
                elMatcher.capture(el)
            }
        }
    }

    override fun toString() = "VarargMatcher(all=$all, prefix=$prefix, postfix=$postfix, centralPart=lambda)"
}

fun CompositeMatcher<*>.captureSubMatchers(arg: Any?) {
    subMatchers?.let {
        it
            .filterIsInstance<CapturingMatcher>()
            .forEach { matcher -> matcher.capture(arg) }
    }
}

/**
 * any<T>() that carries type information so InvocationMatcher can apply checkType()
 *
 * This matcher matches any value, but its type-checking is important for verification/stubbing.
 * It also supports Kotlin value classes where the runtime argument may be represented by the
 * underlying type (boxed/unboxed forms).
 */
data class AnyTypedMatcher(
    override val argumentType: KClass<*>,
) : Matcher<Any>,
    TypedMatcher,
    EquivalentMatcher {
    private val underlyingBoxed: KClass<*>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (!argumentType.isValue) return@lazy null
        argumentType.constructors
            .firstOrNull()
            ?.parameters
            ?.singleOrNull()
            ?.type
            ?.classifier as? KClass<*>
    }

    override fun match(arg: Any?): Boolean = true

    override fun equivalent(): Matcher<Any> = this

    override fun checkType(arg: Any?): Boolean {
        if (arg == null) return true
        if (argumentType.simpleName == null) return true

        if (argumentType.isInstance(arg)) return true

        val expectedBoxed = argumentType.boxedClass
        if (expectedBoxed.isInstance(arg)) return true

        val normalizedArg = arg.boxedValue
        if (argumentType.isInstance(normalizedArg)) return true
        if (expectedBoxed.isInstance(normalizedArg)) return true

        val ub = underlyingBoxed
        if (ub != null) {
            val ubBoxed = ub.boxedClass
            if (ubBoxed.isInstance(arg)) return true
            if (ubBoxed.isInstance(normalizedArg)) return true
        }

        return false
    }

    override fun toString(): String = "any<${argumentType.simpleName}>()"
}
