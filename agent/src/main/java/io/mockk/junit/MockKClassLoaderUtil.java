package io.mockk.junit;

import io.mockk.agent.MockKClassLoader;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class MockKClassLoaderUtil {
    public static final ClassLoader CLASS_LOADER = MockKClassLoader.newClassLoader(MockKClassLoaderUtil.class.getClassLoader());

    @SuppressWarnings("unused") // use from generated class
    public static final Constructor getConstructor(Constructor<?> constructor) {
        try {
            Class<?> cls = constructor.getDeclaringClass();
            Class<?> newCls = CLASS_LOADER.loadClass(cls.getName());
            for (Constructor ct : newCls.getDeclaredConstructors()) {
                if (areSame(ct.getParameterTypes(), constructor.getParameterTypes())) {
                    return ct;
                }
            }
        } catch (Throwable throwable) {
            // skip
        }
        return constructor;
    }

    private static boolean areSame(Class<?>[] params1, Class<?>[] params2) {
        if (params1.length != params2.length) {
            return false;
        }
        for (int i = 0; i < params1.length; i++) {
            if (!params1[i].getName().equals(params2[i].getName())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unused") // use from generated class
    public static Method getMethod(Method method) {
        try {
            Class<?> cls = method.getDeclaringClass();
            Class<?> newCls = CLASS_LOADER.loadClass(cls.getName());
            for (Method mt : newCls.getDeclaredMethods()) {
                if (mt.getName().equals(method.getName()) &&
                        areSame(mt.getParameterTypes(), method.getParameterTypes())) {
                    return mt;
                }
            }
        } catch (Throwable throwable) {
            // skip
        }
        return method;
    }
}
