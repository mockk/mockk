package com.ninjasquad.springmockk

/**
 * Beans created using MockK.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @author Jean-Baptiste Nizet
 */
internal class MockBeans {

    private val beans = mutableListOf<Any>();

    internal fun add(bean: Any) {
        this.beans.add(bean);
    }

    /**
     * Clear all MockK beans configured with the supplied {@link MockkClear} strategy.
     * <p>No mocks will be reset if the supplied strategy is {@link MockkClear#NONE}.
     */
    internal fun clearAll(clear: MockkClear) {
        if (clear != MockkClear.NONE) {
            this.beans.forEach { bean ->
                if (clear == MockkClear.get(bean)) {
                    io.mockk.clearMocks(bean);
                }
            }
        }
    }
}
