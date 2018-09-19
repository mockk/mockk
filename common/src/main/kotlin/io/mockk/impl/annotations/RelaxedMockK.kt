package io.mockk.impl.annotations

/**
 * Creates a relaxed mockk.
 *
 * Example:
 * ```
 * @RelaxedMockK
 * @AdditionalInterface(Runnable::class)
 * private lateinit var relaxedCar: Car
 * ```
 *
 * Requires MockKAnnotations.init() being called on an object
 * declaring variable with this this annotation.
 *
 * As an option to MockKAnnotations.init for JUnit 5 check [io.mockk.junit5.MockKExtension]
 *
 * @param name name of a relaxed mockk
 */
annotation class RelaxedMockK(val name: String = "")