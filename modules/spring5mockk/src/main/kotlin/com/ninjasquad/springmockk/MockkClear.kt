package com.ninjasquad.springmockk

import java.util.IdentityHashMap

/**
 * Clear strategy used on a mockk bean, applied to a mock via the
 * [MockkBean] annotation.
 *
 * @author Phillip Webb
 * @author JB Nizet
 * @since 1.4.0
 * @see ClearMocksTestExecutionListener
 */
enum class MockkClear {
    /**
     * Reset the mock before the test method runs.
     */
    BEFORE,

    /**
     * Reset the mock after the test method runs.
     */
    AFTER,

    /**
     * Don't reset the mock.
     */
    NONE;

    companion object {
        // this has to be an identity hash map. If it's a HashMap, then hashCode() is called on the mocks,
        // and confirmVerified calls fail. See https://github.com/Ninja-Squad/springmockk/issues/27
        // and see MockkClearIntegrationTests
        private val clearModesByMock = IdentityHashMap<Any, MockkClear>()

        internal fun set(mock: Any, clear: MockkClear) {
            require(mock.isMock) { "Only mocks can be cleared" }
            clearModesByMock.put(mock, clear)
        }

        /**
         * Get the [MockkClear] associated with the given mock.
         * @param mock the source mock
         * @return the clear type
         */
        fun get(mock: Any): MockkClear {
            return clearModesByMock[mock] ?: NONE
        }
    }
}

fun <T: Any> T.clear(clear: MockkClear): T {
    MockkClear.set(this, clear)
    return this
}
