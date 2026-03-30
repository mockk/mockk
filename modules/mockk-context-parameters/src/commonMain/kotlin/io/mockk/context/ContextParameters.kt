@file:Suppress("CONTEXT_RECEIVERS_DEPRECATED", "SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")

package io.mockk.context

import io.mockk.MockKMatcherScope

/**
 * Helper functions for stubbing and verifying functions that use Kotlin context parameters.
 *
 * When a function under test uses context parameters, MockK needs matchers for those
 * context arguments. These `withContext` overloads supply `any()` matchers for context
 * parameters while keeping the stub/verify block readable.
 *
 * ## Usage
 *
 * Given a function with context parameters:
 * ```kotlin
 * context(Logger, Raise<DomainError>)
 * fun fetchUser(id: String): User { ... }
 * ```
 *
 * Stub it:
 * ```kotlin
 * every {
 *     withContext<Logger, Raise<DomainError>> {
 *         fetchUser("123")
 *     }
 * } returns mockUser
 * ```
 *
 * Verify it:
 * ```kotlin
 * verify {
 *     withContext<Logger, Raise<DomainError>> {
 *         fetchUser("123")
 *     }
 * }
 * ```
 *
 * ## How it works
 *
 * Context receivers compile to regular function parameters at the JVM level.
 * A `context(C1) () -> Any?` lambda is `Function1<C1, Any?>` in bytecode.
 * These helpers invoke the lambda with `any()` matchers, letting MockK record the call.
 */

/** Provides a single context parameter matcher. */
inline fun <reified C1 : Any> MockKMatcherScope.withContext(
    noinline stubBlock: context(C1) () -> Any?
): Any? = stubBlock(any())

/** Provides two context parameter matchers. */
inline fun <reified C1 : Any, reified C2 : Any> MockKMatcherScope.withContext(
    noinline stubBlock: context(C1, C2) () -> Any?
): Any? = stubBlock(any(), any())

/** Provides three context parameter matchers. */
inline fun <reified C1 : Any, reified C2 : Any, reified C3 : Any> MockKMatcherScope.withContext(
    noinline stubBlock: context(C1, C2, C3) () -> Any?
): Any? = stubBlock(any(), any(), any())

/** Provides four context parameter matchers. */
inline fun <reified C1 : Any, reified C2 : Any, reified C3 : Any, reified C4 : Any> MockKMatcherScope.withContext(
    noinline stubBlock: context(C1, C2, C3, C4) () -> Any?
): Any? = stubBlock(any(), any(), any(), any())

/** Provides five context parameter matchers. */
inline fun <reified C1 : Any, reified C2 : Any, reified C3 : Any, reified C4 : Any, reified C5 : Any> MockKMatcherScope.withContext(
    noinline stubBlock: context(C1, C2, C3, C4, C5) () -> Any?
): Any? = stubBlock(any(), any(), any(), any(), any())
