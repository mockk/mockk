package io.mockk.impl.annotations

annotation class InjectMockKs(
    val byName: Boolean = true,
    val byType: Boolean = true
)
