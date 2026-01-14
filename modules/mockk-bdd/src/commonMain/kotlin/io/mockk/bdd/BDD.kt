package io.mockk.bdd

import io.mockk.MockKMatcherScope
import io.mockk.MockKStubScope
import io.mockk.MockKVerificationScope
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify

/**
 * Starts a block of stubbing in BDD style. Part of DSL.
 *
 * Used to define what behaviour is going to be mocked.
 *
 * Example:
 * ```
 * val navigator = mockk<Navigator>()
 * given { navigator.currentLocation } returns "Home"
 *
 * println(navigator.currentLocation) // prints "Home"
 * ```
 *
 * MockK original function: `io.mockk.every`
 * @see [coGiven] Coroutine version.
 */
fun <T> given(stubBlock: MockKMatcherScope.() -> T): MockKStubScope<T, T> = every(stubBlock)

/**
 * Starts a block of stubbing for coroutines in BDD style. Part of DSL.
 * Similar to [given], but works with suspend functions.
 *
 * Used to define what behaviour is going to be mocked.
 *
 * MockK original function: `io.mockk.coEvery`
 * @see [given]
 */
fun <T> coGiven(stubBlock: suspend MockKMatcherScope.() -> T): MockKStubScope<T, T> = coEvery(stubBlock)

/**
 * Verifies calls happened in the past in BDD style. Part of DSL
 *
 * @param ordering how the verification should be ordered
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 * @param atLeast verifies that the behaviour happened at least [atLeast] times
 * @param atMost verifies that the behaviour happened at most [atMost] times
 * @param exactly verifies that the behaviour happened exactly [exactly] times. Use -1 to disable
 * @param timeout timeout value in milliseconds. Will wait until one of two following states: either verification is
 * passed or timeout is reached.
 * @param verifyBlock code block containing at least 1 call to verify
 *
 * Example:
 * ```
 * val navigator = mockk<Navigator>(relaxed = true)
 *
 * navigator.navigateTo("Park")
 * then { navigator.navigateTo(any()) }
 * ```
 *
 * MockK original function: `io.mockk.verify`
 * @see [coThen] Coroutine version
 */
fun then(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    timeout: Long = 0,
    verifyBlock: MockKVerificationScope.() -> Unit,
) = verify(ordering, inverse, atLeast, atMost, exactly, timeout, verifyBlock)

/**
 * Verifies that calls were made inside a coroutine in BDD style.
 *
 * @param ordering how the verification should be ordered
 * @param inverse when true, the verification will check that the behaviour specified did **not** happen
 * @param atLeast verifies that the behaviour happened at least [atLeast] times
 * @param atMost verifies that the behaviour happened at most [atMost] times
 * @param exactly verifies that the behaviour happened exactly [exactly] times. Use -1 to disable
 * @param timeout timeout value in milliseconds. Will wait until one of two following states: either verification is
 * passed or timeout is reached.
 * @param verifyBlock code block containing at least 1 call to verify
 *
 * MockK original function: `io.mockk.coVerify`
 * @see [then]
 */
fun coThen(
    ordering: Ordering = Ordering.UNORDERED,
    inverse: Boolean = false,
    atLeast: Int = 1,
    atMost: Int = Int.MAX_VALUE,
    exactly: Int = -1,
    timeout: Long = 0,
    verifyBlock: suspend MockKVerificationScope.() -> Unit,
) = coVerify(ordering, inverse, atLeast, atMost, exactly, timeout, verifyBlock)
