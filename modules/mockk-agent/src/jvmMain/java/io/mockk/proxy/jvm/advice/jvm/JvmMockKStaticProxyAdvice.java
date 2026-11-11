package io.mockk.proxy.jvm.advice.jvm;

import io.mockk.proxy.jvm.advice.BaseAdvice;
import io.mockk.proxy.jvm.advice.JdkInternalCallGuard;
import io.mockk.proxy.jvm.advice.ProxyAdviceId;
import io.mockk.proxy.jvm.dispatcher.JvmMockKDispatcher;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.*;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.implementation.bytecode.assign.Assigner.Typing.DYNAMIC;

@SuppressWarnings({"unused", "UnusedAssignment"})
public class JvmMockKStaticProxyAdvice extends BaseAdvice {
    public JvmMockKStaticProxyAdvice(MockHandlerMap handlers) {
        super(handlers);
    }

    /**
     * Calls the JDK makes to itself (for example <code>ZipFile</code> reading a classpath jar
     * through <code>Files.readAttributes</code>) must reach the original implementation even
     * while the class is statically mocked: routing them into the mock handler re-enters the
     * JDK's own classpath machinery, which deadlocks kotlin-reflect and other lazy
     * initializations that read the same classpath (#1402). Application code is unaffected.
     */
    @Override
    public Callable<?> handler(Object self, Method method, Object[] arguments) {
        Callable<?> intercepted = super.handler(self, method, arguments);

        // super.handler() only returns an interceptor while a static mock is active, so the
        // (relatively costly) caller inspection below never runs for pass-through calls.
        if (intercepted != null && JdkInternalCallGuard.INSTANCE.isJdkInternalCall((Class<?>) self, method)) {
            return null;
        }

        return intercepted;
    }

    @OnMethodEnter(skipOn = OnNonDefaultValue.class)
    private static Callable<?> enterStatic(@ProxyAdviceId long id,
                                           @Origin final Method method,
                                           @AllArguments final Object[] arguments) throws Throwable {
        Object self = method.getDeclaringClass();
        JvmMockKDispatcher dispatcher = JvmMockKDispatcher.get(id, self);
        if (dispatcher == null) {
            return null;
        }
        return dispatcher.handler(self, method, arguments);
    }

    @OnMethodExit
    private static void exit(@Advice.Return(readOnly = false, typing = DYNAMIC) Object returned,
                             @Advice.Enter Callable<?> mocked) throws Throwable {
        if (mocked != null) {
            returned = mocked.call();
        }
    }

}
