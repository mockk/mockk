package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.MethodCall;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Advice for proxies built in place of an instance of a hidden class (e.g. a lambda compiled
 * by Kotlin 2 with `invokedynamic`). Hidden classes cannot be subclassed, so the proxy
 * implements the hidden class's interfaces and keeps the original instance in a dedicated
 * field; un-stubbed ("call original") requests are forwarded to that instance.
 */
public class JvmMockKDelegateProxyInterceptor extends BaseAdvice {
    public static final String ORIGINAL_FIELD = "mockkOriginal";

    public JvmMockKDelegateProxyInterceptor(MockHandlerMap handlers) {
        super(handlers);
    }

    @RuntimeType
    public static Object intercept(
            @ProxyAdviceId long id,
            @This Object self,
            @Origin Method method,
            @AllArguments Object[] args,
            @FieldValue(ORIGINAL_FIELD) Object original
    ) throws Throwable {
        JvmMockKDispatcher dispatcher = get(id, self);

        if (dispatcher == null) {
            return null;
        }

        Callable<Object> originalCall =
                original == null ? null : new MethodCall(original, method, args);

        return dispatcher.handle(self, method, args, originalCall);
    }
}
