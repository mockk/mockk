package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface MockKInvocationHandler {
    Object invocation(Object self,
                      Method method,
                      Callable<?> originalCall,
                      Object[] args) throws Exception;
}
