package io.mockk.impl.eval

/**
 * Whether the caught exception is the compiler-inserted
 * `KotlinNothingValueException` that ends a lambda containing an expression of type
 * [kotlin.Nothing] (see RecordedBlockEvaluator for why that terminates a recorded block).
 *
 * The exception class is `internal` in the common stdlib, so the check is platform-specific:
 * on the JVM it is an exact type match; elsewhere recorded blocks keep failing as before.
 */
internal expect fun isNothingExpressionTermination(e: Exception): Boolean
