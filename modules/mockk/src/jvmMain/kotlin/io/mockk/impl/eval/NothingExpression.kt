package io.mockk.impl.eval

/**
 * `KotlinNothingValueException` is `internal` in the Kotlin 2.x stdlib (unreferencable from
 * Kotlin code outside the `kotlin` package since 1.5), but the class itself is shipped with
 * every stdlib distribution, so the check goes by runtime class name.
 */
internal actual fun isNothingExpressionTermination(e: Exception): Boolean = e.javaClass.name == "kotlin.KotlinNothingValueException"
