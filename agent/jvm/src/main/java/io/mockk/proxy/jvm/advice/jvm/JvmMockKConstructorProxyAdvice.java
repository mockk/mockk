package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.MockKInvocationHandler;
import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import net.bytebuddy.asm.Advice;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("unused")
public class JvmMockKConstructorProxyAdvice extends BaseAdvice {
    public JvmMockKConstructorProxyAdvice(@NotNull Map<Object, ? extends MockKInvocationHandler> handlers) {
        super(handlers);
    }

    @Advice.OnMethodExit
    private static void exit(
            @ProxyAdviceId long id,
            @Advice.This Object self,
            @Advice.AllArguments final Object[] arguments
    ) {
        JvmMockKDispatcher dispatcher = JvmMockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return;
        }

        dispatcher.constructorDone(self, arguments);
    }
}
