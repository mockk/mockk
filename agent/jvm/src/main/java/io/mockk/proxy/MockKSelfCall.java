package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MockKSelfCall {
    public static final ThreadLocal<Object> SELF_CALL = new ThreadLocal<Object>();
    public static final ThreadLocal<Method> SELF_CALL_METHOD = new ThreadLocal<Method>();

    public static boolean isSelf(Object self, Method method) {
        return SELF_CALL.get() == self
                && checkOverride(SELF_CALL_METHOD.get(), method);
    }

    private static boolean checkOverride(Method method1, Method method2) {
        return method1.getName().equals(method2.getName())
                && Arrays.equals(method1.getParameterTypes(), method2.getParameterTypes());
    }
}
