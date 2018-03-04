package io.mockk.proxy;

import net.bytebuddy.asm.Advice.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

@SuppressWarnings({"unused", "UnusedAssignment"})
/*
 * Workaround #35
 */
public class MockKHashMapStaticProxyAdvice extends MockKProxyDispatcher {
    public MockKHashMapStaticProxyAdvice(Map<Object, MockKInvocationHandler> handlers) {
        super(handlers);
    }

    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    private static Callable<?> enterStatic(@MockKProxyAdviceId long id,
                                           @Origin final Method method,
                                           @AllArguments final Object[] arguments) throws Throwable {
        if (arguments.length == 1 && arguments[0] == HashMap.class) {
            return null;
        }
        Object self = method.getDeclaringClass();
        MockKDispatcher dispatcher = MockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return null;
        }
        return dispatcher.handle(self, method, arguments);
    }

    @OnMethodExit
    private static void exit(@Return(readOnly = false, typing = DYNAMIC) Object returned,
                             @Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

}
