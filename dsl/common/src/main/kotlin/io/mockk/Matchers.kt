package io.mockk

import io.mockk.InternalPlatformDsl.toArray
import io.mockk.InternalPlatformDsl.toStr
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * Matcher that checks equality. By reference and by value (equals method)
 */
data class EqMatcher<in T : Any>(private val valueArg: T, val ref: Boolean = false, val inverse: Boolean = false) :
    Matcher<T> {
    val value = InternalPlatformDsl.unboxChar(valueArg)

    override fun match(arg: T?): Boolean {
        val result = if (ref) {
            arg === value
        } else {
            if (arg == null) {
                false
            } else {
                val unboxedArg = InternalPlatformDsl.unboxChar(arg)
                InternalPlatformDsl.deepEquals(unboxedArg, value)
            }
        }
        return if (inverse) !result else result
    }

    override fun substitute(map: Map<Any, Any>) =
        copy(valueArg = valueArg.internalSubstitute(map))

    override fun toString(): String {
        return if (ref)
            "${if (inverse) "refNonEq" else "refEq"}(${value.toStr()})"
        else
            "${if (inverse) "nonEq" else "eq"}(${value.toStr()})"
    }
}

/**
 * Matcher that always returns one same value.
 */
data class ConstantMatcher<in T : Any>(val constValue: Boolean) : Matcher<T> {
    override fun match(arg: T?): Boolean = constValue

    override fun toString(): String = if (constValue) "any()" else "none()"
}

/**
 * Delegates matching to lambda function
 */
data class FunctionMatcher<in T : Any>(
    val matchingFunc: (T?) -> Boolean,
    override val argumentType: KClass<*>
) : Matcher<T>, TypedMatcher, EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher<Any>(true)

    override fun match(arg: T?): Boolean = matchingFunc(arg)

    override fun toString(): String = "matcher<${argumentType.simpleName}>()"
}

/**
 * Matcher capturing all results to the list.
 */
data class CaptureMatcher<T : Any>(
    val captureList: MutableList<T>,
    override val argumentType: KClass<*>
) : Matcher<T>, CapturingMatcher, TypedMatcher, EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher<Any>(true)

    @Suppress("UNCHECKED_CAST")
    override fun capture(arg: Any?) {
        captureList.add(arg as T)
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "capture<${argumentType.simpleName}>()"
}

/**
 * Matcher capturing all results to the list. Allows nulls
 */
data class CaptureNullableMatcher<T : Any>(
    val captureList: MutableList<T?>,
    override val argumentType: KClass<*>
) : Matcher<T>, CapturingMatcher, TypedMatcher, EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher<Any>(true)

    @Suppress("UNCHECKED_CAST")
    override fun capture(arg: Any?) {
        captureList.add(arg as T?)
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "captureNullable<${argumentType.simpleName}>()"
}

/**
 * Matcher capturing one last value to the CapturingSlot
 */
data class CapturingSlotMatcher<T : Any>(
    val captureSlot: CapturingSlot<T>,
    override val argumentType: KClass<*>
) : Matcher<T>, CapturingMatcher, TypedMatcher, EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher<Any>(true)

    @Suppress("UNCHECKED_CAST")
    override fun capture(arg: Any?) {
        if (arg == null) {
            captureSlot.isNull = true
        } else {
            captureSlot.isNull = false
            captureSlot.captured = arg as T
        }
        captureSlot.isCaptured = true
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "slotCapture<${argumentType.simpleName}>()"
}

/**
 * Matcher comparing values
 */
data class ComparingMatcher<T : Comparable<T>>(
    val value: T,
    val cmpFunc: Int,
    override val argumentType: KClass<T>
) : Matcher<T>, TypedMatcher {
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

    override fun substitute(map: Map<Any, Any>) =
        copy(value = value.internalSubstitute(map))

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
    val second: T
) : Matcher<T>, CompositeMatcher<T>, CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(first, second)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T?): Boolean =
        if (and)
            subMatchers!![0].match(arg) && subMatchers!![1].match(arg)
        else
            subMatchers!![0].match(arg) || subMatchers!![1].match(arg)

    override fun substitute(map: Map<Any, Any>): Matcher<T> {
        val matcher = copy(
            first = first.internalSubstitute(map),
            second = second.internalSubstitute(map)
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
        return if (sm != null)
            "$op(${sm[0]}, ${sm[1]})"
        else
            "$op()"
    }


}

/**
 * Boolean logic "NOT" matcher composed of one matcher
 */
data class NotMatcher<T : Any>(val value: T) : Matcher<T>, CompositeMatcher<T>, CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(value)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T?): Boolean =
        !subMatchers!![0].match(arg)

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
        return if (sm != null)
            "not(${sm[0]})"
        else
            "not()"
    }
}

/**
 * Checks if argument is null or non-null
 */
data class NullCheckMatcher<in T : Any>(val inverse: Boolean = false) : Matcher<T> {
    override fun match(arg: T?): Boolean = if (inverse) arg != null else arg == null

    override fun toString(): String {
        return if (inverse)
            "notNull()"
        else
            "null()"
    }
}

/**
 * Checks matcher data type
 */
data class OfTypeMatcher<in T : Any>(val cls: KClass<*>) : Matcher<T> {
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
class InvokeMatcher<in T : Any>(val block: (T) -> Unit) : Matcher<T>, EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher<Any>(true)

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
 * Checks if assertion is true
 */
class AssertMatcher<in T : Any>(
    val assertFunction: (T?) -> Boolean,
    val msg: String? = null,
    override val argumentType: KClass<*>,
    val nullable: Boolean = false
) : Matcher<T>, TypedMatcher, EquivalentMatcher {
    override fun equivalent(): Matcher<Any> = ConstantMatcher<Any>(true)

    override fun checkType(arg: Any?): Boolean {
        if (arg != null && !argumentType.isInstance(arg)) {
            val argType = arg::class.simpleName
            val requiredType = argumentType.simpleName
            throw AssertionError(
                "Verification matcher assertion failed:\n" +
                        "    type <$argType> is not matching\n" +
                        "    required by assertion type <$requiredType>\n"
            )
        }
        return true
    }

    override fun match(arg: T?): Boolean {
        if (!nullable) {
            if (arg == null) {
                throw AssertionError("Verification matcher assertion failed: null passed to non-nullable assert")
            }
        }
        if (!assertFunction(arg)) {
            throw AssertionError(
                "Verification matcher assertion failed" +
                        (if (msg != null) ": $msg" else "")
            )
        }
        return true
    }

    override fun toString(): String = "assert<${argumentType.simpleName}>()"
}

/**
 * Matcher that can match arrays via provided matchers for each element.
 */
data class ArrayMatcher<in T : Any>(private val matchers: List<Matcher<Any>>) : Matcher<T>, CapturingMatcher {

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

    override fun substitute(map: Map<Any, Any>) =
        copy(matchers = matchers.map { it.substitute(map) })

    override fun toString(): String {
        return matchers.joinToString(prefix = "[", postfix = "]")
    }
}


fun CompositeMatcher<*>.captureSubMatchers(arg: Any?) {
    subMatchers?.let {
        it.filterIsInstance<CapturingMatcher>()
            .forEach { it.capture(arg) }
    }
}

