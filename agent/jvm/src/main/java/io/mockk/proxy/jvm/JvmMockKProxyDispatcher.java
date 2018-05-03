package io.mockk.proxy.jvm;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;

import io.mockk.agent.MockKInvocationHandler;

public class JvmMockKProxyDispatcher extends JvmMockKDispatcher {
    private static final Random RNG = new Random();
    private final long id = RNG.nextLong();

    private final Map<Object, MockKInvocationHandler> handlers;

    public JvmMockKProxyDispatcher(Map<Object, MockKInvocationHandler> handlers) {
        this.handlers = handlers;
    }

    public long getId() {
        return id;
    }

    @Override
    public Callable<?> handler(final Object self, Method method, final Object[] arguments) throws Exception {
        final MockKInvocationHandler handler = handlers.get(self);
        if (handler == null) {
            return null;
        }
        if (MockKSelfCall.isSelf(self, method)) {
            return null;
        }

        return new MockKCallProxy(handler, self, method, arguments);
    }

    @Override
    public Object handle(Object self, Method method, Object[] arguments, Callable<Object> originalMethod) throws Exception {
        final MockKInvocationHandler handler = handlers.get(self);
        if (handler == null) {
            return callIfNotNull(originalMethod);
        }

        if (MockKSelfCall.isSelf(self, method)) {
            return callIfNotNull(originalMethod);
        }

        return handler(self, method, arguments).call();
    }

    private Object callIfNotNull(Callable<Object> originalMethod) throws Exception {
        return originalMethod != null ? originalMethod.call() : null;
    }

}
