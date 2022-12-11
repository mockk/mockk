package io.mockk.impl.annotations

/**
 * Creates a mockk.
 *
 * Example:
 *
 * ```
 * @MockK
 * @AdditionalInterface(Runnable::class)
 * private lateinit var car: Car
 * ```
 *
 * Requires MockKAnnotations.init() being called on an object
 * declaring variable with this annotation.
 *
 * As an option to `MockKAnnotations.init` for JUnit 5 check [io.mockk.junit5.MockKExtension]
 *
 * @param name name of a mockk
 * @param relaxed make it relaxed, an alternative to [RelaxedMockK]
 * @param relaxUnitFun make it relaxed only for unit returning functions
 *
 */
annotation class MockK(
    val name: String = "",
    val relaxed: Boolean = false,
    val relaxUnitFun: Boolean = false
)
