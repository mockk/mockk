/*
 * Copyright (c) 2016 MockK contributors
 * This program is made available under the terms of the MIT License.
 */

package io.mockk.proxy.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend for the method entry hooks. Checks if the hooks should cause an interception or should
 * be ignored.
 */
class MockMethodAdvice {
    private final Map<Object, InvocationHandlerAdapter> interceptors;

    /**
     * Pattern to decompose a instrumentedMethodWithTypeAndSignature
     */
    private final Pattern methodPattern = Pattern.compile("(.*)#(.*)\\((.*)\\)");

    @SuppressWarnings("ThreadLocalUsage")
    private final SelfCallInfo selfCallInfo = new SelfCallInfo();

    MockMethodAdvice(Map<Object, InvocationHandlerAdapter> interceptors) {
        this.interceptors = interceptors;
    }

    /**
     * Try to invoke the method {@code origin} on {@code instance}.
     *
     * @param origin    method to invoke
     * @param instance  instance to invoke the method on.
     * @param arguments arguments to the method
     * @return result of the method
     * @throws Throwable Exception if thrown by the method
     */
    private static Object tryInvoke(Method origin, Object instance, Object[] arguments)
            throws Throwable {
        try {
            return origin.invoke(instance, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    /**
     * Remove calls to a class from a throwable's stack.
     *
     * @param throwable  throwable to clean
     * @param current    stack frame number to start cleaning from (upwards)
     * @param targetType class to remove from the stack
     * @return throwable with the cleaned stack
     */
    private static Throwable hideRecursiveCall(Throwable throwable, int current,
                                               Class<?> targetType) {
        try {
            StackTraceElement[] stack = throwable.getStackTrace();
            int skip = 0;
            StackTraceElement next;

            do {
                next = stack[stack.length - current - ++skip];
            } while (!next.getClassName().equals(targetType.getName()));

            int top = stack.length - current - skip;
            StackTraceElement[] cleared = new StackTraceElement[stack.length - skip];
            System.arraycopy(stack, 0, cleared, 0, top);
            System.arraycopy(stack, top + skip, cleared, top, current);
            throwable.setStackTrace(cleared);

            return throwable;
        } catch (RuntimeException ignored) {
            // This should not happen unless someone instrumented or manipulated exception stack
            // traces.
            return throwable;
        }
    }

    /**
     * Get the method of {@code instance} specified by {@code methodWithTypeAndSignature}.
     *
     * @param instance                   instance the method belongs to
     * @param methodWithTypeAndSignature the description of the method
     * @return method {@code methodWithTypeAndSignature} refer to
     */
    @SuppressWarnings("unused")
    public Method getOrigin(Object instance, String methodWithTypeAndSignature) throws Throwable {
        if (!isMocked(instance)) {
            return null;
        }

        Matcher methodComponents = methodPattern.matcher(methodWithTypeAndSignature);
        boolean wasFound = methodComponents.find();
        if (!wasFound) {
            throw new IllegalArgumentException();
        }
        String argTypeNames[] = methodComponents.group(3).split(",");

        ArrayList<Class<?>> argTypes = new ArrayList<>(argTypeNames.length);
        for (String argTypeName : argTypeNames) {
            if (argTypeName.equals("")) {
                continue;
            }

            argTypes.add(Util.nameToType(argTypeName));
        }

        Method origin = Class.forName(methodComponents.group(1)).getDeclaredMethod(
                methodComponents.group(2), argTypes.toArray(new Class<?>[]{}));

        if (isOverridden(instance, origin)) {
            return null;
        } else {
            return origin;
        }
    }


    /**
     * Handle a method entry hook.
     *
     * @param instance  instance that is mocked
     * @param origin    method that contains the hook
     * @param arguments arguments to the method
     * @return A callable that can be called to get the mocked result or null if the method is not
     * mocked.
     */
    @SuppressWarnings("unused")
    public Callable<?> handle(Object instance, Method origin, Object[] arguments) throws Throwable {
        InvocationHandlerAdapter interceptor = interceptors.get(instance);
        if (interceptor == null) {
            return null;
        }

        return new ReturnValueWrapper(interceptor.interceptEntryHook(instance, origin, arguments,
                new SuperMethodCall(selfCallInfo, origin, instance, arguments)));
    }

    /**
     * Checks if an {@code instance} is a mock.
     *
     * @param instance instance that might be a mock
     * @return {@code true} iff the instance is a mock
     */
    public boolean isMock(Object instance) {
        return interceptors.containsKey(instance);
    }

    /**
     * Check if this method call should be mocked. Usually the same as {@link #isMock(Object)} but
     * takes into account the state of {@link #selfCallInfo} that allows to temporary disable
     * mocking for a single method call.
     *
     * @param instance instance that might be mocked
     * @return {@code true} iff the a method call should be mocked
     * @see SelfCallInfo
     */
    public boolean isMocked(Object instance) {
        return selfCallInfo.shouldMockMethod(instance) && isMock(instance);
    }

    /**
     * Check if a method is overridden.
     *
     * @param instance mocked instance
     * @param origin   method that might be overridden
     * @return {@code true} iff the method is overridden
     */
    public boolean isOverridden(Object instance, Method origin) {
        Class<?> currentType = instance.getClass();

        do {
            try {
                return !origin.equals(currentType.getDeclaredMethod(origin.getName(),
                        origin.getParameterTypes()));
            } catch (NoSuchMethodException ignored) {
                currentType = currentType.getSuperclass();
            }
        } while (currentType != null);

        return true;
    }

    /**
     * Used to call the read (non mocked) method.
     */
    private static class SuperMethodCall implements InvocationHandlerAdapter.SuperMethod {
        private final SelfCallInfo selfCallInfo;
        private final Method origin;
        private final Object instance;
        private final Object[] arguments;

        private SuperMethodCall(SelfCallInfo selfCallInfo, Method origin, Object instance,
                                Object[] arguments) {
            this.selfCallInfo = selfCallInfo;
            this.origin = origin;
            this.instance = instance;
            this.arguments = arguments;
        }

        /**
         * Call the read (non mocked) method.
         *
         * @return Result of read method
         * @throws Throwable thrown by the read method
         */
        @Override
        public Object invoke() throws Throwable {
            if (!Modifier.isPublic(origin.getDeclaringClass().getModifiers()
                    & origin.getModifiers())) {
                origin.setAccessible(true);
            }

            // By setting instance in the the selfCallInfo, once single method call on this instance
            // and thread will call the read method as isMocked will return false.
            selfCallInfo.set(instance);
            return tryInvoke(origin, instance, arguments);
        }

    }

    /**
     * Stores a return value of {@link #handle(Object, Method, Object[])} and returns in on
     * {@link #call()}.
     */
    private static class ReturnValueWrapper implements Callable<Object> {
        private final Object returned;

        private ReturnValueWrapper(Object returned) {
            this.returned = returned;
        }

        @Override
        public Object call() {
            return returned;
        }
    }

    /**
     * Used to call the original method. If a instance is {@link #set(Object)}
     * {@link #shouldMockMethod(Object)} returns false for this instance once.
     *
     * <p>This is {@link ThreadLocal}, so a thread can {@link #set(Object)} and instance and then
     * call {@link #shouldMockMethod(Object)} without interference.
     *
     * @see SuperMethodCall#invoke()
     * @see #isMocked(Object)
     */
    private static class SelfCallInfo extends ThreadLocal<Object> {
        boolean shouldMockMethod(Object value) {
            Object current = get();

            if (current == value) {
                set(null);
                return false;
            } else {
                return true;
            }
        }
    }
}
