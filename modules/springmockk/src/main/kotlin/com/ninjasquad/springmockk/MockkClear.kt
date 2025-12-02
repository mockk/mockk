package com.ninjasquad.springmockk

import java.lang.ref.WeakReference


/**
 * Clear strategy used on a mock bean.
 *
 * Usually applied to a mock via the [`@MockkBean`][MockkBean] or
 * [`@MockkSpyBean`][MockkSpyBean] annotation but can also be directly
 * applied to any mock in the `ApplicationContext` using the [clear] extension function.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 * @see MockkClearTestExecutionListener
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
     * Do not reset the mock.
     */
    NONE;

    companion object {
        private data class MockkClearEntry(
            val mockRef: WeakReference<Any>,
            var clearMode: MockkClear
        )

        // An identity hashmap would be more efficient and was used before.
        // But it would retain references to mocks that aren't used anymore (because the Spring context cache
        // has a limit on the number of cached contexts). See https://github.com/Ninja-Squad/springmockk/issues/97
        // A weak hashmap would be ideal, but it uses equals and hashCode, which would cause hashCode() to be called on the mocks,
        // and confirmVerified calls to fail. See https://github.com/Ninja-Squad/springmockk/issues/27
        // and see MockkClearIntegrationTests
        private val entries = mutableListOf<MockkClearEntry>()

        internal fun set(mock: Any, clear: MockkClear) {
            require(mock.isMockOrSpy) { "Only mocks can be cleared" }
            // Using === is important here to not call equals() on the mock.
            val entry = entries.firstOrNull { it.mockRef.refersTo(mock) }?.apply { clearMode = clear }
            if (entry == null) {
                entries.add(MockkClearEntry(WeakReference(mock), clear))
            }
        }

        /**
         * Get the [MockkClear] associated with the given mock.
         * @param mock the source mock
         * @return the clear type
         */
        fun get(mock: Any): MockkClear {
            return entries.firstOrNull { it.mockRef.refersTo(mock) }?.clearMode ?: NONE
        }
    }
}

/**
 * Sets the clear mode for the given mock or spy
 */
fun <T : Any> T.clear(clear: MockkClear): T {
    MockkClear.set(this, clear)
    return this
}
