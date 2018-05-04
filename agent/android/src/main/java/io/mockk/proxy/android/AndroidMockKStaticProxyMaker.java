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

import android.os.Build;
import io.mockk.agent.MockKAgentException;
import io.mockk.agent.MockKInvocationHandler;
import io.mockk.agent.MockKStaticProxyMaker;

import java.io.IOException;
import java.util.HashMap;

/**
 * Adds stubbing hooks to static methods
 *
 * <p>This is done by transforming the byte code of the classes to add method entry hooks.
 */
public final class AndroidMockKStaticProxyMaker implements MockKStaticProxyMaker {
    /**
     * {@link StaticJvmtiAgent} set up during one time init
     */
    private static final StaticJvmtiAgent AGENT;

    /**
     * Error  during one time init or {@code null} if init was successful
     */
    private static final Throwable INITIALIZATION_ERROR;

    /*
     * One time setup to allow the system to mocking via this mock maker.
     */
    static {
        StaticJvmtiAgent agent;
        Throwable initializationError = null;

        try {
            try {
                agent = new StaticJvmtiAgent();
            } catch (IOException ioe) {
                throw new IllegalStateException("Mockito could not self-attach a jvmti agent to " +
                        "the current VM. This feature is required for inline mocking.\nThis error" +
                        " occured due to an I/O error during the creation of this agent: " + ioe
                        + "\n\nPotentially, the current VM does not support the jvmti API " +
                        "correctly", ioe);
            }
        } catch (Throwable throwable) {
            agent = null;
            initializationError = throwable;
        }

        AGENT = agent;
        INITIALIZATION_ERROR = initializationError;
    }

    /**
     * Maps class to handler.
     */
    private final HashMap<Class, InvocationHandlerAdapter> classToHandler = new HashMap<>();

    /**
     * Class doing the actual byte code transformation.
     */
    private final StaticClassTransformer classTransformer;

    /**
     * Create a new mock maker.
     */
    public AndroidMockKStaticProxyMaker() {
        if (INITIALIZATION_ERROR != null) {
            throw new MockKAgentException("Could not initialize static inline mock maker.\n" + "\n" +
                    "Release: Android " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL
                    + "Device: " + Build.BRAND + " " + Build.MODEL, INITIALIZATION_ERROR);
        }

        classTransformer = new StaticClassTransformer(
                AGENT,
                AndroidMockKProxyMaker.DISPATCHER_CLASS,
                classToHandler
        );
    }

    @Override
    public void staticProxy(Class<?> clazz, MockKInvocationHandler handler) {
        InvocationHandlerAdapter handlerAdapter = new InvocationHandlerAdapter(handler);
        classTransformer.mockClass(clazz);
        classToHandler.put(clazz, handlerAdapter);
    }

    @Override
    public void staticUnProxy(Class<?> clazz) {
        classToHandler.remove(clazz);
    }
}
