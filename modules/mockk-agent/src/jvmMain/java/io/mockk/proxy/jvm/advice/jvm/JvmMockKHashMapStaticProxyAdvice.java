package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import net.bytebuddy.asm.Advice.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.Callable;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

@SuppressWarnings({"unused", "UnusedAssignment"})
/*
 * Workaround #35
 */
public class JvmMockKHashMapStaticProxyAdvice extends BaseAdvice {
    public JvmMockKHashMapStaticProxyAdvice(MockHandlerMap handlers) {
        super(handlers);
    }

    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    private static Callable<?> enterStatic(@ProxyAdviceId long id,
                                           @Origin final Method method,
                                           @AllArguments final Object[] arguments) throws Throwable {
        if (arguments.length == 1 && arguments[0] == HashMap.class) {
            return null;
        }
        Object self = method.getDeclaringClass();
        JvmMockKDispatcher dispatcher = JvmMockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return null;
        }
        return dispatcher.handler(self, method, arguments);
    }

    @OnMethodExit
    private static void exit(@Return(readOnly = false, typing = DYNAMIC) Object returned,
                             @Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

}
