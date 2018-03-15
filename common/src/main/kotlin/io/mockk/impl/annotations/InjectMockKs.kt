package io.mockk.impl.annotations

annotation class InjectMockKs(
    val type: InjectType = InjectType.BOTH
)

enum class InjectType {
    BY_NAME, BY_TYPE, BOTH;

    val byName
        get() = this == BY_NAME || this == BOTH

    val byType
        get() = this == BY_TYPE || this == BOTH
}
