package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MockKDispatcher {
    private static final Map<Long, MockKDispatcher> DISPATCHER_MAP = new ConcurrentHashMap<Long, MockKDispatcher>();

    public static MockKDispatcher get(long id, Object obj) {
        if (obj == DISPATCHER_MAP) {
            return null;
        }

        return DISPATCHER_MAP.get(id);
    }

    public static void set(long id, MockKDispatcher dispatcher) {
        DISPATCHER_MAP.put(id, dispatcher);
    }

    public abstract Callable<?> handle(Object self,
                                       Method method,
                                       Object[] arguments) throws Exception;
}
