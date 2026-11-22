package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import io.mockk.proxy.jvm.advice.OriginMethodResolver;
import io.mockk.proxy.jvm.util.DefaultInterfaceMethodResolver;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class JvmMockKProxyInterceptor extends BaseAdvice {
    public JvmMockKProxyInterceptor(MockHandlerMap handlers) {
        super(handlers);
    }

    @RuntimeType
    @BindingPriority(BindingPriority.DEFAULT * 2)
    public static Object intercept(@ProxyAdviceId long id,
                                   @This Object self,
                                   @Origin Method method,
                                   @Origin String origin,
                                   @AllArguments Object[] args,
                                   @SuperCall final Callable<Object> originalMethod) throws Throwable {
        JvmMockKDispatcher dispatcher = get(id, self);

        if (dispatcher == null) {
            return originalMethod.call();
        }

        return dispatcher.handle(self, OriginMethodResolver.INSTANCE.resolve(method, origin), args, originalMethod);
    }

    @RuntimeType
    public static Object interceptNoSuper(@ProxyAdviceId long id,
                                          @This Object self,
                                          @Origin Method method,
                                          @Origin String origin,
                                          @AllArguments Object[] args) throws Throwable {
        JvmMockKDispatcher dispatcher = get(id, self);

        if (dispatcher == null) {
            return null;
        }

        Method trueMethod = OriginMethodResolver.INSTANCE.resolve(method, origin);
        return dispatcher.handle(self, trueMethod, args, DefaultInterfaceMethodResolver.Companion.getDefaultImplementationOrNull$mockk_agent(self, trueMethod, args));

    }

}