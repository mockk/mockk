/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package io.mockk.proxy.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Backend for the method entry hooks. Checks if the hooks should cause an interception or should
 * be ignored.
 */
class AndroidMockKMethodAdvice {
    private final Map<Object, MockKInvocationHandlerAdapter> interceptors;

    @SuppressWarnings("ThreadLocalUsage")
    private final SelfCallInfo selfCallInfo = new SelfCallInfo();

    AndroidMockKMethodAdvice(Map<Object, MockKInvocationHandlerAdapter> interceptors) {
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
    private static Object tryInvoke(
            Method origin,
            Object instance,
            Object[] arguments
    ) throws Throwable {
        try {
            return origin.invoke(instance, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

//    /**
//     * Remove calls to a class from a throwable's stack.
//     *
//     * @param throwable  throwable to clean
//     * @param current    stack frame number to start cleaning from (upwards)
//     * @param targetType class to remove from the stack
//     * @return throwable with the cleaned stack
//     */
//    private static Throwable hideRecursiveCall(Throwable throwable, int current,
//                                               Class<?> targetType) {
//        try {
//            StackTraceElement[] stack = throwable.getStackTrace();
//            int skip = 0;
//            StackTraceElement next;
//
//            do {
//                next = stack[stack.length - current - ++skip];
//            } while (!next.getClassName().equals(targetType.getName()));
//
//            int top = stack.length - current - skip;
//            StackTraceElement[] cleared = new StackTraceElement[stack.length - skip];
//            System.arraycopy(stack, 0, cleared, 0, top);
//            System.arraycopy(stack, top + skip, cleared, top, current);
//            throwable.setStackTrace(cleared);
//
//            return throwable;
//        } catch (RuntimeException ignored) {
//            // This should not happen unless someone instrumented or manipulated exception stack
//            // traces.
//            return throwable;
//        }
//    }

    /**
     * Would a call to SubClass.method handled by SuperClass.method ?
     * <p>This is the case when subclass or any intermediate parent does not override method.
     *
     * @param subclass         Class that might have been called
     * @param superClass       Class defining the method
     * @param methodName       Name of method
     * @param methodParameters Parameter of method
     * @return {code true} iff the method would have be handled by superClass
     */
    private static boolean isMethodDefinedBySuperClass(Class<?> subclass, Class<?> superClass,
                                                       String methodName,
                                                       Class<?>[] methodParameters) {
        do {
            if (subclass == superClass) {
                // The method is not overridden in the subclass or any class in between subClass
                // and superClass.
                return true;
            }

            try {
                subclass.getDeclaredMethod(methodName, methodParameters);

                // method is overridden is sub-class. hence the call could not have handled by
                // the super-class.
                return false;
            } catch (NoSuchMethodException e) {
                subclass = subclass.getSuperclass();
            }
        } while (subclass != null);

        // Subclass is not a sub class of superClass
        return false;
    }

    private static List<Class<?>> getAllSubclasses(Class<?> superClass, Collection<Class>
            possibleSubClasses) {
        ArrayList<Class<?>> subclasses = new ArrayList<>();
        for (Class<?> possibleSubClass : possibleSubClasses) {
            if (superClass.isAssignableFrom(possibleSubClass)) {
                subclasses.add(possibleSubClass);
            }
        }

        return subclasses;
    }

    private synchronized static native String nativeGetCalledClassName();

    private Class<?> getClassMethodWasCalledOn(MethodDescriptor methodDesc) throws ClassNotFoundException,
            NoSuchMethodException {
        Class<?> classDeclaringMethod = Util.classForTypeName(methodDesc.className);

        /* If a sub-class does not override a static method, the super-classes method is called
         * directly. Hence 'classDeclaringMethod' will be the super class. As the mocking of
         * this and the class actually called might be different we need to find the class that
         * was actually called.
         */
        if (Modifier.isFinal(classDeclaringMethod.getModifiers())) {
            return classDeclaringMethod;
        } else if (Modifier.isFinal(
                classDeclaringMethod.getDeclaredMethod(
                        methodDesc.methodName,
                        methodDesc.methodParamTypes
                ).getModifiers())) {
            return classDeclaringMethod;
        } else {
            boolean mightBeMocked = false;
            // if neither the defining class nor any subclass of it is mocked, no point of
            // trying to figure out the called class as isMocked will soon be checked.
            for (Class<?> subClass : getAllSubclasses(classDeclaringMethod, getClassMocks())) {
                if (isMethodDefinedBySuperClass(subClass, classDeclaringMethod,
                        methodDesc.methodName, methodDesc.methodParamTypes)) {
                    mightBeMocked = true;
                    break;
                }
            }

            if (!mightBeMocked) {
                return null;
            }

            String calledClassName = nativeGetCalledClassName();
            return Class.forName(calledClassName);
        }
    }

    private Collection<Class> getClassMocks() {
        Collection<Class> classes = new HashSet<>();
        for (Object mock : interceptors.keySet()) {
            if (mock instanceof Class<?>) {
                classes.add((Class) mock);
            }
        }
        return classes;
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
        MethodDescriptor methodDesc = new MethodDescriptor(methodWithTypeAndSignature);
        if (instance == null) {
            Class clazz = getClassMethodWasCalledOn(methodDesc);
            if (clazz == null) {
                return null;
            }

            if (!isMocked(clazz)) {
                return null;
            }

            return Class.forName(methodDesc.className)
                    .getDeclaredMethod(
                            methodDesc.methodName,
                            methodDesc.methodParamTypes
                    );
        } else {
            if (!isMocked(instance)) {
                return null;
            }

            Method origin = Class.forName(methodDesc.className)
                    .getDeclaredMethod(
                            methodDesc.methodName,
                            methodDesc.methodParamTypes
                    );


            if (isOverridden(instance, origin)) {
                return null;
            } else {
                return origin;
            }
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

        if (Modifier.isStatic(origin.getModifiers())) {
            MethodDescriptor methodDesc = new MethodDescriptor((String) instance);
            instance = getClassMethodWasCalledOn(methodDesc);
        }

        MockKInvocationHandlerAdapter interceptor = interceptors.get(instance);
        if (interceptor == null) {
            return null;
        }

        SuperMethodCall superMethodCall = new SuperMethodCall(selfCallInfo, origin, instance, arguments);
        Object result = interceptor.interceptEntryHook(instance, origin, arguments, superMethodCall);
        return new ReturnValueWrapper(result);
    }

    /**
     * Checks if an {@code instance} is a mock.
     *
     * @param instance instance that might be a mock
     * @return {@code true} iff the instance is a mock
     */
    @SuppressWarnings("unused") // called from JNI
    public boolean isMock(Object instance) {
        if (instance == interceptors) {
            return false;
        }
        return interceptors.containsKey(instance);
    }

    /**
     * Check if this method call should be mocked.
     */
    public boolean isMocked(Object instance) {
        return selfCallInfo.shouldMockMethod(instance);
    }

    /**
     * Check if a method is overridden.
     *
     * @param instance mocked instance
     * @param origin   method that might be overridden
     * @return {@code true} iff the method is overridden
     */
    private boolean isOverridden(Object instance, Method origin) {
        Class<?> currentType = instance.getClass();

        do {
            try {

                Method method = currentType.getDeclaredMethod(
                        origin.getName(),
                        origin.getParameterTypes()
                );

                return !methodEquals(origin, method);

            } catch (NoSuchMethodException ignored) {
                currentType = currentType.getSuperclass();
            }
        } while (currentType != null);

        return true;
    }

    @SuppressWarnings("StringEquality")
    public static boolean methodEquals(Method method1, Method method2) {
        if (method1.getDeclaringClass() != method2.getDeclaringClass()) {
            return false;
        }
        if (method1.getName() != method2.getName()) {
            return false;
        }
        if (method1.getReturnType() != method2.getReturnType()) {
            return false;
        }

        Class<?>[] params1 = method1.getParameterTypes();
        Class<?>[] params2 = method2.getParameterTypes();

        if (params1.length != params2.length) {
            return false;
        }

        for (int i = 0; i < params1.length; i++) {
            if (params1[i] != params2[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Used to call the real (non mocked) method.
     */
    private static class SuperMethodCall implements MockKInvocationHandlerAdapter.SuperMethod {
        private final SelfCallInfo selfCallInfo;
        private final Method origin;
        private final Object instance;
        private final Object[] arguments;

        private SuperMethodCall(
                SelfCallInfo selfCallInfo,
                Method origin,
                Object instance,
                Object[] arguments
        ) {
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
            int modifiers = origin.getDeclaringClass().getModifiers() & origin.getModifiers();

            if (!Modifier.isPublic(modifiers)) {
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
