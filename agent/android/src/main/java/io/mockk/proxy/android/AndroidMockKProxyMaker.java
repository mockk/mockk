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

import com.android.dx.stock.ProxyBuilder;
import com.android.dx.stock.ProxyBuilder.MethodSetEntry;
import io.mockk.agent.*;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generates mock instances on Android's runtime that can mock final methods.
 *
 * <p>This is done by transforming the byte code of the classes to add method entry hooks.
 */

public final class AndroidMockKProxyMaker implements MockKProxyMaker {
    private final MockKInstantiatior instantiatior;

    /**
     * All currently active mocks. We modify the class's byte code. Some objects of the class are
     * modified, some are not. This list helps the {@link AndroidMockKMethodAdvice} help figure out if a
     * object's method calls should be intercepted.
     */
    private final Map<Object, MockKInvocationHandlerAdapter> mocks;

    /**
     * Class doing the actual byte code transformation.
     */
    private final AndroidMockKClassTransformer classTransformer;

    /**
     * Create a new mock maker.
     */
    public AndroidMockKProxyMaker(
            MockKInstantiatior instantiatior,
            AndroidMockKClassTransformer classTransformer,
            Map<Object, MockKInvocationHandlerAdapter> mocks
    ) {
        this.instantiatior = instantiatior;
        this.classTransformer = classTransformer;
        this.mocks = mocks;
    }

    /**
     * Get methods to proxy.
     *
     * <p>Only abstract methods will need to get proxied as all other methods will get an entry
     * hook.
     *
     * @return methods to proxy.
     */
    private <T> Method[] getMethodsToProxy(Class<T> clazz, Class<?>[] interfaces) {
        Set<MethodSetEntry> abstractMethods = new HashSet<>();
        Set<MethodSetEntry> nonAbstractMethods = new HashSet<>();

        Class<?> superClass = clazz;
        while (superClass != null) {
            for (Method method : superClass.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())
                        && !nonAbstractMethods.contains(new MethodSetEntry(method))) {
                    abstractMethods.add(new MethodSetEntry(method));
                } else {
                    nonAbstractMethods.add(new MethodSetEntry(method));
                }
            }

            superClass = superClass.getSuperclass();
        }

        for (Class<?> i : clazz.getInterfaces()) {
            for (Method method : i.getMethods()) {
                if (!nonAbstractMethods.contains(new MethodSetEntry(method))) {
                    abstractMethods.add(new MethodSetEntry(method));
                }
            }
        }

        for (Class<?> i : interfaces) {
            for (Method method : i.getMethods()) {
                if (!nonAbstractMethods.contains(new MethodSetEntry(method))) {
                    abstractMethods.add(new MethodSetEntry(method));
                }
            }
        }

        Method[] methodsToProxy = new Method[abstractMethods.size()];
        int i = 0;
        for (MethodSetEntry entry : abstractMethods) {
            methodsToProxy[i++] = entry.originalMethod;
        }

        return methodsToProxy;
    }

    @Override
    public <T> Cancelable<T> proxy(
            Class<T> clazz,
            Class<?>[] interfaces,
            MockKInvocationHandler handler,
            boolean useDefaultConstructor,
            final Object instance) {
        MockKInvocationHandlerAdapter handlerAdapter = new MockKInvocationHandlerAdapter(handler);

        if (instance != null) {
            if (classTransformer == null) {
                throw new MockKAgentException("Mocking objects is supported starting from Android P");
            }
            classTransformer.mockClass(clazz, interfaces);
            mocks.put(instance, handlerAdapter);
            return new CancelableResult<T>((T) instance, new Function0<Unit>() {
                @Override
                public Unit invoke() {
                    mocks.remove(instance);
                    return Unit.INSTANCE;
                }
            });
        }

        final T mock;
        if (clazz.isInterface()) {
            // support interfaces via java.lang.reflect.Proxy
            Class[] classesToMock = new Class[interfaces.length + 1];
            classesToMock[0] = clazz;
            System.arraycopy(interfaces, 0, classesToMock, 1, interfaces.length);

            // newProxyInstance returns the type of typeToMock
            Object proxyInstance = Proxy.newProxyInstance(
                    clazz.getClassLoader(),
                    classesToMock,
                    handlerAdapter
            );

            mock = clazz.cast(proxyInstance);
        } else if (classTransformer != null) {
            boolean subclassingRequired = interfaces.length > 0
                    || Modifier.isAbstract(clazz.getModifiers());

            // Add entry hooks to non-abstract methods.
            classTransformer.mockClass(clazz, interfaces);

            Class<? extends T> proxyClass;

            if (subclassingRequired) {
                try {
                    // support abstract methods via dexmaker's ProxyBuilder
                    proxyClass = ProxyBuilder.forClass(clazz)
                            .implementing(interfaces)
                            .onlyMethods(getMethodsToProxy(clazz, interfaces))
                            .withSharedClassLoader()
                            .buildProxyClass();

                } catch (Exception e) {
                    throw new MockKAgentException("Failed to mock " + clazz, e);
                }

                mock = instantiatior.instance(proxyClass);

                ProxyBuilder.setInvocationHandler(mock, handlerAdapter);
            } else {
                mock = instantiatior.instance(clazz);
            }
        } else {
            if (Modifier.isFinal(clazz.getModifiers())) {
                throw new MockKAgentException("Mocking final classes is supported starting from Android P");
            }
            try {
                Class<? extends T> proxyClass = ProxyBuilder.forClass(clazz)
                        .implementing(interfaces)
                        .buildProxyClass();

                mock = instantiatior.instance(proxyClass);

                ProxyBuilder.setInvocationHandler(mock, handlerAdapter);
            } catch (Exception e) {
                throw new MockKAgentException("Failed to mock " + clazz, e);
            }
        }

        mocks.put(mock, handlerAdapter);
        return new CancelableResult<T>(mock, new Function0<Unit>() {
            @Override
            public Unit invoke() {
                mocks.remove(mock);
                return Unit.INSTANCE;
            }
        });
    }
}
