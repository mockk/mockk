package io.mockk

import io.mockk.external.toStr
import java.util.*

/**
 * Matcher that checks equality. By reference and by value (equals method)
 */
data class EqMatcher<T>(val value: T, val ref: Boolean = false) : Matcher<T> {
    override fun match(arg: T?): Boolean =
            if (ref) {
                arg === value
            } else {
                Objects.deepEquals(arg, value)
            }

    override fun toString(): String =
            if (ref)
                "refEq(${value.toStr()})"
            else
                "eq(${value.toStr()})"
}

/**
 * Matcher that always returns one same value.
 */
data class ConstantMatcher<in T>(val constValue: Boolean) : Matcher<T> {
    override fun match(arg: T?): Boolean = constValue

    override fun toString(): String = if (constValue) "any()" else "none()"
}

/**
 * Delegating matching to lambda function
 */
data class FunctionMatcher<T>(val matchingFunc: (T?) -> Boolean) : Matcher<T> {
    override fun match(arg: T?): Boolean = matchingFunc(arg)

    override fun toString(): String = "matcher()"
}

/**
 * Matcher capturing all results to the list.
 */
data class CaptureMatcher<T>(val captureList: MutableList<T>) : Matcher<T>, CapturingMatcher {
    override fun capture(arg: Any?) {
        captureList.add(arg as T)
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "capture()"
}

/**
 * Matcher capturing all results to the list. Allows nulls
 */
data class CaptureNullableMatcher<T>(val captureList: MutableList<T?>) : Matcher<T>, CapturingMatcher {
    override fun capture(arg: Any?) {
        captureList.add(arg as T?)
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "captureNullable()"
}

/**
 * Matcher capturing one last value to the CapturingSlot
 */
data class CapturingSlotMatcher<T>(val captureSlot: CapturingSlot<T>) : Matcher<T>, CapturingMatcher {
    override fun capture(arg: Any?) {
        captureSlot.captured = arg as T?
    }

    override fun match(arg: T?): Boolean = true

    override fun toString(): String = "slotCapture()"
}

/**
 * Matcher comparing values
 */
data class ComparingMatcher<T : Comparable<T>>(val value: T, val cmpFunc: Int) : Matcher<T> {
    override fun match(arg: T?): Boolean {
        if (arg == null) return false
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

    override fun match(arg: T?): Boolean =
            if (and)
                subMatchers!![0].match(arg) && subMatchers!![1].match(arg)
            else
                subMatchers!![0].match(arg) || subMatchers!![1].match(arg)

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
data class NotMatcher<T>(val value: T) : Matcher<T>, CompositeMatcher<T>, CapturingMatcher {
    override val operandValues: List<T>
        get() = listOf(value)

    override var subMatchers: List<Matcher<T>>? = null

    override fun match(arg: T?): Boolean =
            !subMatchers!![0].match(arg)

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
data class NullCheckMatcher<T>(val inverse: Boolean) : Matcher<T> {
    override fun match(arg: T?): Boolean = if (inverse) arg != null else arg == null

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
    override fun match(arg: T?): Boolean = cls.isInstance(arg)

    override fun toString() = "ofType(${cls.name})"
}

/**
 * Matcher to replace all unspecified argument matchers to any()
 * Handled by logic in a special way
 */
data class AllAnyMatcher<T>(val fake: Int) : Matcher<T> {
    override fun match(arg: T?): Boolean = true

    override fun toString() = "allAny()"
}

