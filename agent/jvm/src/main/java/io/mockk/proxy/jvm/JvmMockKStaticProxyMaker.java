package io.mockk.proxy.jvm;

import io.mockk.agent.MockKAgentException;
import io.mockk.agent.MockKInvocationHandler;
import io.mockk.agent.MockKStaticProxyMaker;

import java.util.ArrayList;

public class JvmMockKStaticProxyMaker implements MockKStaticProxyMaker {
    private MockKInstrumentation instrumentation;

    public JvmMockKStaticProxyMaker(MockKInstrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public void staticProxy(Class<?> clazz,
                            MockKInvocationHandler handler) {
        JvmMockKProxyMaker.log.debug("Injecting handle to " + clazz + " for static methods");

        ArrayList<Class<?>> lst = new ArrayList<Class<?>>();
        lst.add(clazz);
        boolean transformed = instrumentation.inject(lst);
        if (!transformed) {
            throw new MockKAgentException("Failed to create static proxy for " + clazz + ".\n" +
                    "Try running VM with MockK Java Agent\n" +
                    "i.e. with -javaagent:mockk-agent.jar option.");
        }

        instrumentation.hookStatic(clazz, handler);
    }

    @Override
    public void staticUnProxy(Class<?> clazz) {
        instrumentation.unhookStatic(clazz);
    }
}
