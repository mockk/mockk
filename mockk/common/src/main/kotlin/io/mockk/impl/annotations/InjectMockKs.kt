package io.mockk.impl.annotations

annotation class InjectMockKs(
    val lookupType: InjectionLookupType = InjectionLookupType.BOTH,
    val injectImmutable: Boolean = false,
    val overrideValues: Boolean = false
)

annotation class OverrideMockKs(
    val lookupType: InjectionLookupType = InjectionLookupType.BOTH,
    val injectImmutable: Boolean = true
)

enum class InjectionLookupType {
    BY_NAME, BY_TYPE, BOTH;

    val byName
        get() = this == BY_NAME || this == BOTH

    val byType
        get() = this == BY_TYPE || this == BOTH
}
