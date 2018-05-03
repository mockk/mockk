package io.mockk.proxy.jvm;

import io.mockk.agent.MockKInvocationHandler;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

public class MockKProxyInterceptor extends JvmMockKProxyDispatcher {
    public MockKProxyInterceptor(Map<Object, MockKInvocationHandler> handlers) {
        super(handlers);
    }

    @RuntimeType
    @BindingPriority(BindingPriority.DEFAULT * 2)
    public static Object intercept(@MockKProxyAdviceId long id,
                                   @This Object self,
                                   @Origin Method method,
                                   @AllArguments Object[] args,
                                   @SuperCall final Callable<Object> originalMethod) throws Throwable {
        JvmMockKDispatcher dispatcher = get(id, self);

        if (dispatcher == null) {
            return originalMethod.call();
        }

        return dispatcher.handle(self, method, args, originalMethod);
    }

    @RuntimeType
    public static Object interceptNoSuper(@MockKProxyAdviceId long id,
                                          @This Object self,
                                          @Origin Method method,
                                          @AllArguments Object[] args) throws Throwable {
        JvmMockKDispatcher dispatcher = get(id, self);

        if (dispatcher == null) {
            return null;
        }

        return dispatcher.handle(self, method, args, null);
    }

}