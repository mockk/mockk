package io.mockk.proxy.jvm;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import io.mockk.agent.MockKInvocationHandler;

public class MockKCallProxy implements Callable<Object> {
    private final MockKInvocationHandler handler;
    private final Object self;
    private final Method method;
    private final Object[] arguments;

    public MockKCallProxy(MockKInvocationHandler handler, Object self, Method method, Object[] arguments) {
        this.handler = handler;
        this.self = self;
        this.method = method;
        this.arguments = arguments;
    }

    @Override
    public Object call() throws Exception {
        MockKCallMethod callOriginal = new MockKCallMethod(self, method, arguments);
        MockKSkipInterceptingSelf skipSelfAndCallOriginal = new MockKSkipInterceptingSelf(callOriginal, self, method);

        return handler.invocation(self, method, skipSelfAndCallOriginal, arguments);
    }

}
