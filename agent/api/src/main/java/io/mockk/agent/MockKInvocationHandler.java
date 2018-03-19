package io.mockk.agent;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface MockKInvocationHandler {
    Object invocation(Object self,
                      Method method,
                      Callable<?> originalCall,
                      Object[] args) throws Exception;
}
