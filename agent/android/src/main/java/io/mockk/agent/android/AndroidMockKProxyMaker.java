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

package io.mockk.agent.android;

import android.os.AsyncTask;
import android.os.Build;
import android.util.ArraySet;
import com.android.dx.stock.ProxyBuilder;
import com.android.dx.stock.ProxyBuilder.MethodSetEntry;
import io.mockk.agent.MockKAgentException;
import io.mockk.agent.MockKInvocationHandler;
import io.mockk.agent.MockKProxyMaker;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Generates mock instances on Android's runtime that can mock final methods.
 *
 * <p>This is done by transforming the byte code of the classes to add method entry hooks.
 */

public final class AndroidMockKProxyMaker implements MockKProxyMaker {
    private static final String DISPATCHER_CLASS_NAME =
            "io.mockk.agent.android.MockMethodDispatcher";
    private static final String DISPATCHER_JAR = "dispatcher.jar";

    /** {@link io.mockk.agent.android.JvmtiAgent} set up during one time init */
    private static final JvmtiAgent AGENT;

    /** Error  during one time init or {@code null} if init was successful*/
    private static final Throwable INITIALIZATION_ERROR;

    /**
     * Class injected into the bootstrap classloader. All entry hooks added to methods will call
     * this class.
     */
    public static final Class DISPATCHER_CLASS;

    /*
     * One time setup to allow the system to mocking via this mock maker.
     */
    static {
        JvmtiAgent agent;
        Throwable initializationError = null;
        Class dispatcherClass = null;
        try {
            try {
                agent = new JvmtiAgent();

                try (InputStream is = AndroidMockKProxyMaker.class.getClassLoader()
                        .getResource(DISPATCHER_JAR).openStream()) {
                    agent.appendToBootstrapClassLoaderSearch(is);
                }

                try {
                    dispatcherClass = Class.forName(DISPATCHER_CLASS_NAME, true,
                            Object.class.getClassLoader());

                    if (dispatcherClass == null) {
                        throw new IllegalStateException(DISPATCHER_CLASS_NAME
                                + " could not be loaded");
                    }
                } catch (ClassNotFoundException cnfe) {
                    throw new IllegalStateException(
                            "MockK failed to inject the MockMethodDispatcher class into the "
                            + "bootstrap class loader\n\nIt seems like your current VM does not "
                            + "support the jvmti API correctly.", cnfe);
                }
            } catch (IOException ioe) {
                throw new IllegalStateException(
                        "MockK could not self-attach a jvmti agent to the current VM. This "
                        + "feature is required for inline mocking.\nThis error occured due to an "
                        + "I/O error during the creation of this agent: " + ioe + "\n\n"
                        + "Potentially, the current VM does not support the jvmti API correctly",
                        ioe);
            }
        } catch (Throwable throwable) {
            agent = null;
            initializationError = throwable;
        }

        AGENT = agent;
        INITIALIZATION_ERROR = initializationError;
        DISPATCHER_CLASS = dispatcherClass;
    }

    /**
     * All currently active mocks. We modify the class's byte code. Some objects of the class are
     * modified, some are not. This list helps the {@link MockMethodAdvice} help figure out if a
     * object's method calls should be intercepted.
     */
    private final Map<Object, InvocationHandlerAdapter> mocks;

    /**
     * Class doing the actual byte code transformation.
     */
    private final ClassTransformer classTransformer;

    /**
     * Create a new mock maker.
     */
    public AndroidMockKProxyMaker() {
        if (INITIALIZATION_ERROR != null) {
            throw new RuntimeException(
                    "Could not initialize inline mock maker.\n"
                    + "\n"
                    + "Release: Android " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL
                    + "Device: " + Build.BRAND + " " + Build.MODEL, INITIALIZATION_ERROR);
        }

        mocks = new MockMap();
        classTransformer = new ClassTransformer(AGENT, DISPATCHER_CLASS, mocks);
    }

    /**
     * Get methods to proxy.
     *
     * <p>Only abstract methods will need to get proxied as all other methods will get an entry
     * hook.
     *
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
    public <T> T instance(Class<T> cls) {
        return null;
    }

    @Override
    public <T> T proxy(
            Class<T> clazz,
            Class<?>[] interfaces,
            MockKInvocationHandler handler,
            boolean useDefaultConstructor,
            Object instance) {
        InvocationHandlerAdapter handlerAdapter = new InvocationHandlerAdapter(handler);

        if (instance != null) {
            classTransformer.mockClass(clazz, interfaces);
            mocks.put(instance, handlerAdapter);
            return clazz.cast(instance);
        }

        T mock;
        if (clazz.isInterface()) {
            // support interfaces via java.lang.reflect.Proxy
            Class[] classesToMock = new Class[interfaces.length + 1];
            classesToMock[0] = clazz;
            System.arraycopy(interfaces, 0, classesToMock, 1, interfaces.length);

            // newProxyInstance returns the type of typeToMock
            mock = clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), classesToMock,
                    handlerAdapter));
        } else {
            boolean subclassingRequired = interfaces.length > 0
                    || Modifier.isAbstract(clazz.getModifiers());

            // Add entry hooks to non-abstract methods.
            classTransformer.mockClass(clazz, interfaces);

            Class<? extends T> proxyClass;

            if (subclassingRequired) {
                try {
                    // support abstract methods via dexmaker's ProxyBuilder
                    proxyClass = ProxyBuilder.forClass(clazz).implementing(interfaces)
                            .onlyMethods(getMethodsToProxy(clazz, interfaces)).withSharedClassLoader()
                            .buildProxyClass();

                } catch (Exception e) {
                    throw new MockKAgentException("Failed to mock " + clazz, e);
                }

                mock = instance(proxyClass);

                ProxyBuilder.setInvocationHandler(mock, handlerAdapter);
            } else {
                mock = instance(clazz);
            }
        }

        mocks.put(mock, handlerAdapter);
        return mock;
    }

    @Override
    public void unproxy(Object instance) {
        mocks.remove(instance);
    }

    @Override
    public void staticProxy(Class<?> clazz, MockKInvocationHandler handler) {
        throw new MockKAgentException("static proxy is not supported yet");
    }

    @Override
    public void staticUnProxy(Class<?> clazz) {
        throw new MockKAgentException("static proxy is not supported yet");
    }


    /**
     * A map mock -> adapter that holds weak references to the mocks and cleans them up when a
     * stale reference is found.
     */
    private static class MockMap extends ReferenceQueue<Object>
            implements Map<Object, InvocationHandlerAdapter> {
        private static final int MIN_CLEAN_INTERVAL_MILLIS = 16000;
        private static final int MAX_GET_WITHOUT_CLEAN = 16384;

        private final Object lock = new Object();
        private StrongKey cachedKey;

        private HashMap<WeakKey, InvocationHandlerAdapter> adapters = new HashMap<>();

        /**
         * The time we issues the last cleanup
         */
        long mLastCleanup = 0;

        /**
         * If {@link #cleanStaleReferences} is currently cleaning stale references out of
         * {@link #adapters}
         */
        private boolean isCleaning = false;

        /**
         * The number of time {@link #get} was called without cleaning up stale references.
         * {@link #get} is a method that is called often.
         *
         * We need to do periodic cleanups as we might never look at mocks at higher indexes and
         * hence never realize that their references are stale.
         */
        private int getCount = 0;

        /**
         * Try to get a recycled cached key.
         *
         * @param obj the reference the key wraps
         *
         * @return The recycled cached key or a new one
         */
        private StrongKey createStrongKey(Object obj) {
            synchronized (lock) {
                if (cachedKey == null) {
                    cachedKey = new StrongKey();
                }

                cachedKey.obj = obj;
                StrongKey newKey = cachedKey;
                cachedKey = null;

                return newKey;
            }
        }

        /**
         * Recycle a key. The key should not be used afterwards
         *
         * @param key The key to recycle
         */
        private void recycleStrongKey(StrongKey key) {
            synchronized (lock) {
                cachedKey = key;
            }
        }

        @Override
        public int size() {
            return adapters.size();
        }

        @Override
        public boolean isEmpty() {
            return adapters.isEmpty();
        }

        @SuppressWarnings("CollectionIncompatibleType")
        @Override
        public boolean containsKey(Object mock) {
            synchronized (lock) {
                StrongKey key = createStrongKey(mock);
                boolean containsKey = adapters.containsKey(key);
                recycleStrongKey(key);

                return containsKey;
            }
        }

        @Override
        public boolean containsValue(Object adapter) {
            synchronized (lock) {
                return adapters.containsValue(adapter);
            }
        }

        @SuppressWarnings("CollectionIncompatibleType")
        @Override
        public InvocationHandlerAdapter get(Object mock) {
            synchronized (lock) {
                if (getCount > MAX_GET_WITHOUT_CLEAN) {
                    cleanStaleReferences();
                    getCount = 0;
                } else {
                    getCount++;
                }

                StrongKey key = createStrongKey(mock);
                InvocationHandlerAdapter adapter = adapters.get(key);
                recycleStrongKey(key);

                return adapter;
            }
        }

        /**
         * Remove entries that reference a stale mock from {@link #adapters}.
         */
        private void cleanStaleReferences() {
            synchronized (lock) {
                if (!isCleaning) {
                    if (System.currentTimeMillis() - MIN_CLEAN_INTERVAL_MILLIS < mLastCleanup) {
                        return;
                    }

                    isCleaning = true;

                    AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (lock) {
                                while (true) {
                                    Reference<?> ref = MockMap.this.poll();
                                    if (ref == null) {
                                        break;
                                    }

                                    adapters.remove(ref);
                                }

                                mLastCleanup = System.currentTimeMillis();
                                isCleaning = false;
                            }
                        }
                    });
                }
            }
        }

        @Override
        public InvocationHandlerAdapter put(Object mock, InvocationHandlerAdapter adapter) {
            synchronized (lock) {
                InvocationHandlerAdapter oldValue = remove(mock);
                adapters.put(new WeakKey(mock), adapter);

                return oldValue;
            }
        }

        @SuppressWarnings("CollectionIncompatibleType")
        @Override
        public InvocationHandlerAdapter remove(Object mock) {
            synchronized (lock) {
                StrongKey key = createStrongKey(mock);
                InvocationHandlerAdapter adapter = adapters.remove(key);
                recycleStrongKey(key);

                return adapter;
            }
        }

        @Override
        public void putAll(Map<?, ? extends InvocationHandlerAdapter> map) {
            synchronized (lock) {
                for (Entry<?, ? extends InvocationHandlerAdapter> entry : map.entrySet()) {
                    put(entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public void clear() {
            synchronized (lock) {
                adapters.clear();
            }
        }

        @Override
        public Set<Object> keySet() {
            synchronized (lock) {
                Set<Object> mocks = new ArraySet<>(adapters.size());

                boolean hasStaleReferences = false;
                for (WeakKey key : adapters.keySet()) {
                    Object mock = key.get();

                    if (mock == null) {
                        hasStaleReferences = true;
                    } else {
                        mocks.add(mock);
                    }
                }

                if (hasStaleReferences) {
                    cleanStaleReferences();
                }

                return mocks;
            }
        }

        @Override
        public Collection<InvocationHandlerAdapter> values() {
            synchronized (lock) {
                return adapters.values();
            }
        }

        @Override
        public Set<Entry<Object, InvocationHandlerAdapter>> entrySet() {
            synchronized (lock) {
                Set<Entry<Object, InvocationHandlerAdapter>> entries = new ArraySet<>(
                        adapters.size());

                boolean hasStaleReferences = false;
                for (Entry<WeakKey, InvocationHandlerAdapter> entry : adapters.entrySet()) {
                    Object mock = entry.getKey().get();

                    if (mock == null) {
                        hasStaleReferences = true;
                    } else {
                        entries.add(new AbstractMap.SimpleEntry<>(mock, entry.getValue()));
                    }
                }

                if (hasStaleReferences) {
                    cleanStaleReferences();
                }

                return entries;
            }
        }

        /**
         * A weakly referencing wrapper to a mock.
         *
         * Only equals other weak or strong keys where the mock is the same.
         */
        private class WeakKey extends WeakReference<Object> {
            private final int hashCode;

            private WeakKey(/*@NonNull*/ Object obj) {
                super(obj, MockMap.this);

                // Cache the hashcode as the referenced object might disappear
                hashCode = System.identityHashCode(obj);
            }

            @Override
            public boolean equals(Object other) {
                if (other == this) {
                    return true;
                }

                if (other == null) {
                    return false;
                }

                // Checking hashcode is cheap
                if (other.hashCode() != hashCode) {
                    return false;
                }

                Object obj = get();

                if (obj == null) {
                    cleanStaleReferences();
                    return false;
                }

                if (other instanceof WeakKey) {
                    Object otherObj = ((WeakKey) other).get();

                    if (otherObj == null) {
                        cleanStaleReferences();
                        return false;
                    }

                    return obj == otherObj;
                } else if (other instanceof StrongKey) {
                    Object otherObj = ((StrongKey) other).obj;
                    return obj == otherObj;
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return hashCode;
            }
        }

        /**
         * A strongly referencing wrapper to a mock.
         *
         * Only equals other weak or strong keys where the mock is the same.
         */
        private class StrongKey {
            /*@NonNull*/ private Object obj;

            @Override
            public boolean equals(Object other) {
                if (other instanceof WeakKey) {
                    Object otherObj = ((WeakKey) other).get();

                    if (otherObj == null) {
                        cleanStaleReferences();
                        return false;
                    }

                    return obj == otherObj;
                } else if (other instanceof StrongKey) {
                    return this.obj == ((StrongKey)other).obj;
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(obj);
            }
        }
    }
}
