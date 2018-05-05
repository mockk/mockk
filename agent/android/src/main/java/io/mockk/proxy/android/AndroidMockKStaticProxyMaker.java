/*
 * Copyright (C) 2018 The Android Open Source Project
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

import io.mockk.agent.MockKInvocationHandler;
import io.mockk.agent.MockKStaticProxyMaker;

import java.util.Map;

/**
 * Adds stubbing hooks to static methods
 *
 * <p>This is done by transforming the byte code of the classes to add method entry hooks.
 */
public final class AndroidMockKStaticProxyMaker implements MockKStaticProxyMaker {
    private final AndroidMockKClassTransformer classTransformer;
    private final Map<Object, MockKInvocationHandlerAdapter> mocks;

    /**
     * Create a new mock maker.
     */
    public AndroidMockKStaticProxyMaker(
            AndroidMockKClassTransformer classTransformer,
            Map<Object, MockKInvocationHandlerAdapter> mocks
    ) {
        this.classTransformer = classTransformer;
        this.mocks = mocks;
    }

    @Override
    public void staticProxy(Class<?> clazz, MockKInvocationHandler handler) {
        MockKInvocationHandlerAdapter handlerAdapter = new MockKInvocationHandlerAdapter(handler);
        classTransformer.mockClass(clazz, new Class[0]);
        mocks.put(clazz, handlerAdapter);
    }

    @Override
    public void staticUnProxy(Class<?> clazz) {
        mocks.remove(clazz);
    }
}
