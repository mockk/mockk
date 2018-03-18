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

import org.mockito.exceptions.base.MockitoException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import io.mockk.MockKException;

/**
 * Adds entry hooks (that eventually call into
 * {@link MockMethodAdvice#handle(Object, Method, Object[])} to all non-abstract methods of the
 * supplied classes.
 *
 * <p></p>Transforming a class to adding entry hooks follow the following simple steps:
 * <ol>
 * <li>{@link #mockClass(MockFeatures)}</li>
 * <li>{@link JvmtiAgent#requestTransformClasses(Class[])}</li>
 * <li>{@link JvmtiAgent#nativeRetransformClasses(Class[])}</li>
 * <li>agent.cc::Transform</li>
 * <li>{@link JvmtiAgent#runTransformers(ClassLoader, String, Class, ProtectionDomain, byte[])}</li>
 * <li>{@link #transform(Class, byte[])}</li>
 * <li>{@link #nativeRedefine(String, byte[])}</li>
 * </ol>
 *
 */
class ClassTransformer {
    // Some classes are so deeply optimized inside the runtime that they cannot be transformed
    private static final Set<Class<? extends java.io.Serializable>> EXCLUDES = new HashSet<>(
            Arrays.asList(Class.class,
                    Boolean.class,
                    Byte.class,
                    Short.class,
                    Character.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class,
                    String.class));
    private final static Random random = new Random();

    /** Jvmti agent responsible for triggering transformation s*/
    private final JvmtiAgent agent;

    /** Types that have already be transformed */
    private final Set<Class<?>> mockedTypes;

    /**
     * A unique identifier that is baked into the transformed classes. The entry hooks will then
     * pass this identifier to
     * {@code com.android.dx.mockito.inline.MockMethodDispatcher#get(String, Object)} to
     * find the advice responsible for handling the method call interceptions.
     */
    private final String identifier;

    /**
     * We can only have a single transformation going on at a time, hence synchronize the
     * transformation process via this lock.
     *
     * @see #mockClass(MockFeatures)
     */
    private final static Object lock = new Object();

    /**
     * Create a new generator.
     *
     * Creating more than one generator might cause transformations to overwrite each other.
     *
     * @param agent agent used to trigger transformations
     * @param dispatcherClass {@code com.android.dx.mockito.inline.MockMethodDispatcher}
     *                        that will dispatch method calls that might need to get intercepted.
     * @param mocks list of mocked objects. As all objects of a class use the same transformed
     *              bytecode the {@link MockMethodAdvice} needs to check this list if a object is
     *              mocked or not.
     */
    ClassTransformer(JvmtiAgent agent, Class dispatcherClass,
                     Map<Object, InvocationHandlerAdapter> mocks) {
        this.agent = agent;
        mockedTypes = Collections.synchronizedSet(new HashSet<Class<?>>());
        identifier = Long.toString(random.nextLong());
        MockMethodAdvice advice = new MockMethodAdvice(mocks);

        try {
            dispatcherClass.getMethod("set", String.class, Object.class).invoke(null, identifier,
                    advice);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        agent.addTransformer(this);
    }

    /**
     * Trigger the process to add entry hooks to a class (and all its parents).
     *
     * @param features specification what to mock
     */
    <T> void mockClass(MockFeatures<T> features) {
        boolean subclassingRequired = !features.interfaces.isEmpty()
                || Modifier.isAbstract(features.mockedType.getModifiers());

        if (subclassingRequired
                && !features.mockedType.isArray()
                && !features.mockedType.isPrimitive()
                && Modifier.isFinal(features.mockedType.getModifiers())) {
            throw new MockKException("Unsupported settings with this type '"
                    + features.mockedType.getName() + "'", null);
        }

        synchronized (lock) {
            Set<Class<?>> types = new HashSet<>();
            Class<?> type = features.mockedType;

            do {
                boolean wasAdded = mockedTypes.add(type);

                if (wasAdded) {
                    if (!EXCLUDES.contains(type)) {
                        types.add(type);
                    }

                    type = type.getSuperclass();
                } else {
                    break;
                }
            } while (type != null && !type.isInterface());

            if (!types.isEmpty()) {
                try {
                    agent.requestTransformClasses(types.toArray(new Class<?>[types.size()]));
                } catch (UnmodifiableClassException exception) {
                    for (Class<?> failed : types) {
                        mockedTypes.remove(failed);
                    }

                    throw new MockKException("Could not modify all classes " + types, exception);
                }
            }
        }
    }

    /**
     * Add entry hooks to all methods of a class.
     *
     * <p>Called by the agent after triggering the transformation via
     * {@link #mockClass(MockFeatures)}.
     *
     * @param classBeingRedefined class the hooks should be added to
     * @param classfileBuffer original byte code of the class
     *
     * @return transformed class
     */
    byte[] transform(Class<?> classBeingRedefined, byte[] classfileBuffer) throws
            IllegalClassFormatException {
        if (classBeingRedefined == null
                || !mockedTypes.contains(classBeingRedefined)) {
            return null;
        } else {
            try {
                return nativeRedefine(identifier, classfileBuffer);
            } catch (Throwable throwable) {
                throw new IllegalClassFormatException();
            }
        }
    }

    /**
     * Check if the class should be transformed.
     *
     * @param classBeingRedefined The class that might need to transformed
     *
     * @return {@code true} iff the class needs to be transformed
     */
    boolean shouldTransform(Class<?> classBeingRedefined) {
        return classBeingRedefined != null && mockedTypes.contains(classBeingRedefined);
    }

    private native byte[] nativeRedefine(String identifier, byte[] original);
}
