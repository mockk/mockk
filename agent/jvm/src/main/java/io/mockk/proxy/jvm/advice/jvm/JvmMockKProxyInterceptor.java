package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.MockKInvocationHandler;
import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

public class JvmMockKProxyInterceptor extends BaseAdvice {
    public JvmMockKProxyInterceptor(Map<Object, MockKInvocationHandler> handlers) {
        super(handlers);
    }

    @RuntimeType
    @BindingPriority(BindingPriority.DEFAULT * 2)
    public static Object intercept(@ProxyAdviceId long id,
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
    public static Object interceptNoSuper(@ProxyAdviceId long id,
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