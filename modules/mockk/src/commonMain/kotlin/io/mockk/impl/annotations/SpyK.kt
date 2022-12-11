package io.mockk.impl.annotations

/**
 * Creates a spyk.
 *
 * Example:
 *
 * ```
 * @SpyK
 * @AdditionalInterface(Runnable::class)
 * private var carSpy = Car()
 * ```
 *
 * Requires MockKAnnotations.init() being called on an object
 * declaring variable with this annotation.
 *
 * As an option to `MockKAnnotations.init` for JUnit 5 check [io.mockk.junit5.MockKExtension]
 *
 * @param name name of a spyk
 * @param recordPrivateCalls enables recording of private calls
 *
 */
annotation class SpyK(
    val name: String = "",
    val recordPrivateCalls: Boolean = true
)
