package io.mockk.agent.inline;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public interface MockKMethodHandler {
    Object invoke(Object self,
                  Method thisMethod,
                  Callable<?> callRealMethod,
                  Object[] args) throws Exception;
}
