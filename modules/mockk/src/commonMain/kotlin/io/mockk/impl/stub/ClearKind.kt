package io.mockk.impl.stub

/**
 * The kind of mock a [Stub] represents, as `clearAllMocks` groups them.
 */
internal enum class ClearKind {
    REGULAR,
    OBJECT,
    STATIC,
    CONSTRUCTOR,
}

/**
 * The [ClearKind] this stub belongs to, or `null` when `clearAllMocks` never clears it.
 */
internal val Stub.clearKind: ClearKind?
    get() =
        when {
            this is ConstructorStub -> ClearKind.CONSTRUCTOR
            this is SpyKStub<*> ->
                when (mockType) {
                    MockType.SPY -> ClearKind.REGULAR
                    MockType.OBJECT -> ClearKind.OBJECT
                    MockType.STATIC -> ClearKind.STATIC
                    MockType.CONSTRUCTOR -> ClearKind.CONSTRUCTOR
                    else -> null
                }
            this is MockKStub -> ClearKind.REGULAR.takeIf { mockType == MockType.REGULAR }
            else -> null
        }
