package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.Callable;

import static io.mockk.proxy.MockKInvocationHandler.HANDLERS;

public class MockKProxyDispatcher extends MockKDispatcher {
    private static final Random RNG = new Random();
    private final long id = RNG.nextLong();

    public long getId() {
        return id;
    }

    @Override
    public Callable<?> handle(Object self, Method method, Object[] arguments) throws Exception {
        final MockKInvocationHandler handler = HANDLERS.get(self);
        if (handler == null) {
            return null;
        }
        if (MockKSelfCall.SELF_CALL.get() == self) {
            return null;
        }

        return new MockKCallProxy(handler, self, method, arguments);
    }
}
