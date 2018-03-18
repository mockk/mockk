/*
 * Copyright (C) 2012 The Android Open Source Project
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

package io.mockk.agent.android;

import com.android.dx.stock.ProxyBuilder;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationFactory.RealMethodBehavior;
import org.mockito.invocation.MockHandler;
import org.mockito.mock.MockCreationSettings;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static org.mockito.Mockito.withSettings;

/**
 * Handles proxy and entry hook method invocations added by
 * {@link InlineDexmakerMockMaker#createMock(MockCreationSettings, MockHandler)}
 */
final class InvocationHandlerAdapter implements InvocationHandler {
    private MockHandler handler;

    InvocationHandlerAdapter(MockHandler handler) {
        this.handler = handler;
    }

    private static boolean isEqualsMethod(Method method) {
        return method.getName().equals("equals")
                && method.getParameterTypes().length == 1
                && method.getParameterTypes()[0] == Object.class;
    }

    private static boolean isHashCodeMethod(Method method) {
        return method.getName().equals("hashCode")
                && method.getParameterTypes().length == 0;
    }

    /**
     * Intercept a method call. Called <u>before</u> a method is called by the method entry hook.
     *
     * <p>This does the same as {@link #invoke(Object, Method, Object[])} but this handles methods
     * that got and entry hook.
     *
     * @param mock mocked object
     * @param method method that was called
     * @param rawArgs arguments to the method
     * @param superMethod The super method
     *
     * @return mocked result
     * @throws Throwable An exception if thrown
     */
    Object interceptEntryHook(final Object mock, final Method method, final Object[] rawArgs,
                              final SuperMethod superMethod) throws Throwable {
        // args can be null if the method invoked has no arguments, but Mockito expects a non-null
        Object[] args = rawArgs;
        if (rawArgs == null) {
            args = new Object[0];
        }

        return handler.handle(Mockito.framework().getInvocationFactory().createInvocation(mock,
                withSettings().build(mock.getClass()), method, new RealMethodBehavior() {
                    @Override
                    public Object call() throws Throwable {
                        return superMethod.invoke();
                    }
                }, args));
    }

    /**
     * Intercept a method call. Called <u>before</u> a method is called by the proxied method.
     *
     * <p>This does the same as {@link #interceptEntryHook(Object, Method, Object[], SuperMethod)}
     * but this handles proxied methods. We only proxy abstract methods.
     *
     * @param proxy proxies object
     * @param method method that was called
     * @param rawArgs arguments to the method
     *
     * @return mocked result
     * @throws Throwable An exception if thrown
     */
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] rawArgs) throws
            Throwable {
        // args can be null if the method invoked has no arguments, but Mockito expects a non-null
        Object[] args = rawArgs;
        if (rawArgs == null) {
            args = new Object[0];
        }

        if (isEqualsMethod(method)) {
            return proxy == args[0];
        } else if (isHashCodeMethod(method)) {
            return System.identityHashCode(proxy);
        }

        return handler.handle(Mockito.framework().getInvocationFactory().createInvocation(proxy,
                withSettings().build(proxy.getClass().getSuperclass()), method,
                new RealMethodBehavior() {
                    @Override
                    public Object call() throws Throwable {
                        return ProxyBuilder.callSuper(proxy, method, rawArgs);
                    }
                }, args));
    }

    /**
     * Get the handler registered with this adapter.
     *
     * @return handler
     */
    MockHandler getHandler() {
        return handler;
    }

    /**
     * Set a new handler for this adapter.
     */
    void setHandler(MockHandler handler) {
        this.handler = handler;
    }

    /**
     * Interface used to describe a supermethod that can be called.
     */
    interface SuperMethod {
        Object invoke() throws Throwable;
    }
}
