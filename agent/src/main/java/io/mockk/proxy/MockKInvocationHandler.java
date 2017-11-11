package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface MockKInvocationHandler {
    Object invocation(Object self,
                      Method method,
                      Callable<?> originalMethod,
                      Object []args);
}
