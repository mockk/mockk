package io.mockk.agent.inline;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;

public abstract class MockKDispatcher {
    private static final Map<Long, MockKDispatcher> INSTANCE = new ConcurrentHashMap<Long, MockKDispatcher>();
    public static final Map<Class<?>, Boolean> TRANSFORMED_CLASSES = new HashMap<Class<?>, Boolean>();

    public static MockKDispatcher get(long id, Object obj) {
        if (obj == INSTANCE) {
            return null;
        }

        return INSTANCE.get(id);
    }

    public static void set(long id, MockKDispatcher dispatcher) {
        INSTANCE.put(id, dispatcher);
    }

    public abstract Callable<?> handle(Object self, Method origin, Object[] arguments) throws Exception;
}
