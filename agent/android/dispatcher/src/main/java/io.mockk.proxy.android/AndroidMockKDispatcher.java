/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.mockk.proxy.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Called by method entry hooks. Dispatches these hooks to the {@code MockMethodAdvice}.
 */
@SuppressWarnings("unused")
public class AndroidMockKDispatcher {
    // An instance of {@code MockMethodAdvice}
    private final Object mAdvice;

    // All dispatchers for various identifiers
    private static final ConcurrentMap<String, AndroidMockKDispatcher> INSTANCE =
            new ConcurrentHashMap<>();

    private final Method handleMethod;
    private final Method handleConstructorMethod;
    private final Method isMockMethod;
    private final Method getOriginMethod;

    /**
     * Get the dispatcher for a identifier.
     *
     * @param identifier identifier of the dispatcher
     * @param instance   instance that might be mocked
     * @return dispatcher for the identifier
     */
    public static AndroidMockKDispatcher get(String identifier, Object instance) {
        if (instance == INSTANCE) {
            // Avoid endless loop if ConcurrentHashMap was redefined to check for being a mock.
            return null;
        } else {
            return INSTANCE.get(identifier);
        }
    }

    /**
     * Create a new dispatcher.
     *
     * @param advice Advice the dispatcher should call
     */
    private AndroidMockKDispatcher(Object advice) {
        mAdvice = advice;

        handleMethod = getMethod("handle", Object.class, Method.class, Object[].class);
        handleConstructorMethod = getMethod("handleConstructor", Object.class, String.class, Object[].class);
        getOriginMethod = getMethod("getOrigin", Object.class, String.class);
        isMockMethod = getMethod("isMock", Object.class);
    }

    /**
     * Set up a new advice to receive calls for an identifier
     *
     * @param identifier a unique identifier
     * @param advice     advice the dispatcher should call
     */
    public static void set(String identifier, Object advice) {
        INSTANCE.putIfAbsent(identifier, new AndroidMockKDispatcher(advice));
    }


    private Method getMethod(String name, Class... types) {
        try {
            return mAdvice.getClass().getMethod(name, types);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to get method '" + name + "' from advice");
        }
    }

    public Callable<?> handle(Object instance, Method origin, Object[] arguments) throws Throwable {
        try {
            return (Callable<?>) handleMethod.invoke(mAdvice, instance, origin, arguments);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Callable<?> handleConstructor(Object instance, String methodDescriptor, Object[] arguments) throws Throwable {
        try {
            return (Callable<?>) handleConstructorMethod.invoke(mAdvice, instance, methodDescriptor, arguments);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public boolean isMock(Object instance) throws Throwable {
        try {
            return (Boolean) isMockMethod.invoke(mAdvice, instance);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public Method getOrigin(Object mock, String instrumentedMethodWithTypeAndSignature) throws Throwable {
        try {
            return (Method) getOriginMethod.invoke(mAdvice, mock, instrumentedMethodWithTypeAndSignature);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
