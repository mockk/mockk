package io.mockk.proxy.jvm.dispatcher;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public abstract class JvmMockKDispatcher {
    private static final Map<Long, JvmMockKDispatcher> DISPATCHER_MAP = new ConcurrentHashMap<Long, JvmMockKDispatcher>();

    public static JvmMockKDispatcher get(long id, Object obj) {
        if (obj == DISPATCHER_MAP) {
            return null;
        }

        return DISPATCHER_MAP.get(id);
    }

    public static void set(long id, JvmMockKDispatcher dispatcher) {
        DISPATCHER_MAP.put(id, dispatcher);
    }

    public abstract Callable<?> handler(
            Object self,
            Method method,
            Object[] arguments
    ) throws Exception;

    public abstract void constructorDone(
            Object self,
            Object[] arguments
    );

    public abstract Object handle(
            Object self,
            Method method,
            Object[] arguments,
            Callable<Object> originalMethod
    ) throws Exception;

}
