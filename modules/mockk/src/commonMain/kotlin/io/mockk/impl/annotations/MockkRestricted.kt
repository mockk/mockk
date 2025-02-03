package io.mockk.impl.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class MockkRestricted(
    val mode: MockkRestrictedMode = MockkRestrictedMode.WARN,
    val restricted: Array<KClass<*>> = []
)

enum class MockkRestrictedMode {
    WARN,
    EXCEPTION,
}
