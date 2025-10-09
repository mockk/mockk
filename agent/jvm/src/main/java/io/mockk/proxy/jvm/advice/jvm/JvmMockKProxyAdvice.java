package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.MockKInvocationHandler;
import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

@SuppressWarnings({"unused", "UnusedAssignment"})
public class JvmMockKProxyAdvice extends BaseAdvice {
    public JvmMockKProxyAdvice(Map<Object, MockKInvocationHandler> handlers) {
        super(handlers);
    }

    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    private static Callable<?> enter(
            @ProxyAdviceId long id,
            @This Object self,
            @Origin final Method method,
            @AllArguments final Object[] arguments
    ) throws Throwable {

        // workaround for #35
        if (self.getClass() == HashMap.class) {
            if (arguments.length == 1 &&
                    arguments[0] == HashMap.class) {
                return null;
            }
            if (arguments.length == 2 &&
                    arguments[1] == HashMap.class) {
                return null;
            }
        }

        JvmMockKDispatcher dispatcher = JvmMockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return null;
        }

        return dispatcher.handler(self, method, arguments);
    }

    @OnMethodExit
    private static void exit(
            @Advice.Return(readOnly = false, typing = DYNAMIC) Object returned,
            @This Object self,
            @Origin final Method method,
            @Advice.Enter Callable<?> mocked
    ) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }
}
