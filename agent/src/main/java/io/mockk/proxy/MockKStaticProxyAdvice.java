package io.mockk.proxy;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.*;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.Callable;

import static io.mockk.proxy.MockKInvocationHandler.HANDLERS;
import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

@SuppressWarnings({"unused", "UnusedAssignment"})
public class MockKStaticProxyAdvice extends MockKProxyDispatcher {
    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    private static Callable<?> enterStatic(@MockKProxyAdviceId long id,
                                           @Origin final Method method,
                                           @AllArguments final Object[] arguments) throws Throwable {
        Object self = method.getDeclaringClass();
        MockKDispatcher dispatcher = MockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return null;
        }
        return dispatcher.handle(self, method, arguments);
    }

    @OnMethodExit
    private static void exit(@Advice.Return(readOnly = false, typing = DYNAMIC) Object returned,
                             @Advice.Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

}
