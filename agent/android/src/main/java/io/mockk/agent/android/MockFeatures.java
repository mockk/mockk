/*
 * Copyright (c) 2016 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package io.mockk.agent.android;

import java.util.Collections;
import java.util.Set;

class MockFeatures<T> {
    final Class<T> mockedType;
    final Set<Class<?>> interfaces;

    private MockFeatures(Class<T> mockedType, Set<Class<?>> interfaces) {
        this.mockedType = mockedType;
        this.interfaces = Collections.unmodifiableSet(interfaces);
    }

    static <T> MockFeatures<T> withMockFeatures(Class<T> mockedType, Set<Class<?>> interfaces) {
        return new MockFeatures<>(mockedType, interfaces);
    }
}
