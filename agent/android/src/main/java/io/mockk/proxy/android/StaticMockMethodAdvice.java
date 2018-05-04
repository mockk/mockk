/*
 * Copyright (c) 2018 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package io.mockk.proxy.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backend for the method entry hooks. Checks if the hooks should cause an interception or should
 * be ignored.
 */
class StaticMockMethodAdvice {
    /**
     * Pattern to decompose a instrumentedMethodWithTypeAndSignature
     */
    private final static Pattern methodPattern = Pattern.compile("(.*)#(.*)\\((.*)\\)");
    private final Map<Class, InvocationHandlerAdapter> classToHandler;
    @SuppressWarnings("ThreadLocalUsage")
    private final SelfCallInfo selfCallInfo = new SelfCallInfo();

    StaticMockMethodAdvice(Map<Class, InvocationHandlerAdapter> classToHandler) {
        this.classToHandler = classToHandler;
    }

    /**
     * Try to invoke the method {@code origin}.
     *
     * @param origin    method to invoke
     * @param arguments arguments to the method
     * @return result of the method
     * @throws Throwable Exception if thrown by the method
     */
    private static Object tryInvoke(Method origin, Object[] arguments)
            throws Throwable {
        try {
            return origin.invoke(null, arguments);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

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

    private Class<?> getClassMethodWasCalledOn(MethodDesc methodDesc) throws ClassNotFoundException,
            NoSuchMethodException {
        Class<?> classDeclaringMethod = Util.classForTypeName(methodDesc.className);

        /* If a sub-class does not override a static method, the super-classes method is called
         * directly. Hence 'classDeclaringMethod' will be the super class. As the mocking of
         * this and the class actually called might be different we need to find the class that
         * was actually called.
         */
        if (Modifier.isFinal(classDeclaringMethod.getModifiers())
                || Modifier.isFinal(classDeclaringMethod.getDeclaredMethod(methodDesc.methodName,
                methodDesc.methodParamTypes).getModifiers())) {
            return classDeclaringMethod;
        } else {
            boolean mightBeMocked = false;
            // if neither the defining class nor any subclass of it is mocked, no point of
            // trying to figure out the called class as isMocked will soon be checked.
            for (Class<?> subClass : getAllSubclasses(classDeclaringMethod, classToHandler.keySet())) {
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

    /**
     * Get the method specified by {@code methodWithTypeAndSignature}.
     *
     * @param ignored
     * @param methodWithTypeAndSignature the description of the method
     * @return method {@code methodWithTypeAndSignature} refer to
     */
    @SuppressWarnings("unused")
    public Method getOrigin(Object ignored, String methodWithTypeAndSignature) throws Throwable {
        MethodDesc methodDesc = new MethodDesc(methodWithTypeAndSignature);

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
    }

    /**
     * Handle a method entry hook.
     *
     * @param origin    method that contains the hook
     * @param arguments arguments to the method
     * @return A callable that can be called to get the mocked result or null if the method is not
     * mocked.
     */
    @SuppressWarnings("unused")
    public Callable<?> handle(Object methodDescStr, Method origin, Object[] arguments) throws
            Throwable {
        MethodDesc methodDesc = new MethodDesc((String) methodDescStr);
        Class clazz = getClassMethodWasCalledOn(methodDesc);

        InvocationHandlerAdapter interceptor = classToHandler.get(clazz);
        if (interceptor == null) {
            return null;
        }

        return new ReturnValueWrapper(
                interceptor.interceptEntryHook(clazz, origin, arguments,
                        new SuperMethodCall(selfCallInfo, origin, clazz, arguments)));
    }

    /**
     * Check if this method call should be mocked.
     */
    public boolean isMocked(Class<?> clazz) {
        return selfCallInfo.shouldMockMethod(clazz);
    }

    private static class MethodDesc {
        final String className;
        final String methodName;
        final Class<?>[] methodParamTypes;

        private MethodDesc(String methodWithTypeAndSignature) throws ClassNotFoundException {
            Matcher methodComponents = methodPattern.matcher(methodWithTypeAndSignature);
            boolean wasFound = methodComponents.find();
            if (!wasFound) {
                throw new IllegalArgumentException();
            }

            className = methodComponents.group(1);
            methodName = methodComponents.group(2);
            String methodParamTypeNames[] = methodComponents.group(3).split(",");

            ArrayList<Class<?>> methodParamTypesList = new ArrayList<>(methodParamTypeNames.length);
            for (String methodParamName : methodParamTypeNames) {
                if (!methodParamName.equals("")) {
                    methodParamTypesList.add(Util.nameToType(methodParamName));
                }
            }
            methodParamTypes = methodParamTypesList.toArray(new Class<?>[]{});
        }

        @Override
        public String toString() {
            return className + "#" + methodName;
        }
    }

    /**
     * Used to call the real (non mocked) method.
     */
    private static class SuperMethodCall implements InvocationHandlerAdapter.SuperMethod {
        private final SelfCallInfo selfCallInfo;
        private final Method origin;
        private final Class<?> self;
        private final Object[] arguments;

        private SuperMethodCall(SelfCallInfo selfCallInfo, Method origin, Class<?> self,
                                Object[] arguments) {
            this.selfCallInfo = selfCallInfo;
            this.origin = origin;
            this.self = self;
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
            selfCallInfo.set(self);
            return tryInvoke(origin, arguments);
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
