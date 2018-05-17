package io.mockk.proxy.jvm;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class MockKCallMethod implements Callable<Object> {
    private final Object self;
    private final Method method;
    private final Object[] args;

    public MockKCallMethod(Object self, Method method, Object[] args) {
        this.self = self;
        this.method = method;
        this.args = args;
    }

    @Override
    public Object call() throws Exception {
        method.setAccessible(true);
        return method.invoke(self, args);
    }
}
