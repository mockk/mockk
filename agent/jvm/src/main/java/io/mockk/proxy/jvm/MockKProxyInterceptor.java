package io.mockk.proxy.jvm;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import io.mockk.agent.MockKInvocationHandler;

public class MockKProxyInterceptor {
    @RuntimeType
    @BindingPriority(BindingPriority.DEFAULT * 2)
    public static Object intercept(@This Object self,
                                   @Origin Method method,
                                   @AllArguments Object[] args,
                                   @SuperCall final Callable<Object> originalMethod) throws Throwable {

        MockKInvocationHandler handler = MockKInstrumentation.INSTANCE.getHook(self);
        if (handler == null || MockKSelfCall.isSelf(self, method)) {
            return originalMethod.call();
        }
        return handler.invocation(
                self,
                method,
                new MockKSkipInterceptingSelf(originalMethod, self, method),
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