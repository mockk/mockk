package io.mockk.proxy;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class MockKProxyInterceptor {
    @RuntimeType
    @BindingPriority(BindingPriority.DEFAULT * 2)
    public static Object intercept(@This Object self,
                                   @Origin Method method,
                                   @AllArguments Object[] args,
                                   @SuperCall final Callable<Object> originalMethod) throws Throwable {

        MockKInvocationHandler handler = MockKInstrumentation.INSTANCE.getHook(self);
        if (handler == null ||
                MockKSelfCall.SELF_CALL.get() == self) {
            return originalMethod.call();
        }
        return handler.invocation(
                self,
                method,
                new MockKSkipInterceptingSelf(originalMethod, self),
                args);
    }

    @RuntimeType
    public static Object interceptNoSuper(@This Object self,
                                          @Origin Method method,
                                          @AllArguments Object[] args) throws Throwable {
        MockKInvocationHandler handler = MockKInstrumentation.INSTANCE.getHook(self);

        if (handler == null) {
            return null;
        }

        return handler.invocation(
                self,
                method,
                null,
                args);
    }

}