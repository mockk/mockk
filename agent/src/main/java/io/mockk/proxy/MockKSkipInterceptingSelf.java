package io.mockk.proxy;

import java.util.concurrent.Callable;

public class MockKSkipInterceptingSelf implements Callable<Object> {
    private final Callable<Object> callable;
    private Object self;

    public MockKSkipInterceptingSelf(Callable<Object> callable, Object self) {
        this.callable = callable;
        this.self = self;
    }

    @Override
    public Object call() throws Exception {
        Object prev = MockKSelfCall.SELF_CALL.get();
        MockKSelfCall.SELF_CALL.set(self);
        try {
            return callable.call();
        } finally {
            MockKSelfCall.SELF_CALL.set(prev);
        }
    }
}
