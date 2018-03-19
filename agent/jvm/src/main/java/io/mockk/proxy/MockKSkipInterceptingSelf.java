package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class MockKSkipInterceptingSelf implements Callable<Object> {
    private final Callable<Object> callable;
    private Object self;
    private Method method;

    public MockKSkipInterceptingSelf(Callable<Object> callable, Object self, Method method) {
        this.callable = callable;
        this.self = self;
        this.method = method;
    }

    @Override
    public Object call() throws Exception {
        Object prevSelf = MockKSelfCall.SELF_CALL.get();
        Method prevMethod = MockKSelfCall.SELF_CALL_METHOD.get();
        MockKSelfCall.SELF_CALL.set(self);
        MockKSelfCall.SELF_CALL_METHOD.set(method);
        try {
            return callable.call();
        } finally {
            MockKSelfCall.SELF_CALL.set(prevSelf);
            MockKSelfCall.SELF_CALL_METHOD.set(prevMethod);
        }
    }
}
