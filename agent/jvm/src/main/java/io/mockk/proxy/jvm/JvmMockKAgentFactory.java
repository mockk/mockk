package io.mockk.proxy.jvm;

import io.mockk.agent.*;

public class JvmMockKAgentFactory implements MockKAgentFactory {
    private final JvmMockKInstantiatior instantiatior;
    private final JvmMockKProxyMaker proxyMaker;
    private final JvmMockKStaticProxyMaker staticProxyMaker;

    public JvmMockKAgentFactory() {
        MockKInstrumentation instrumentation = new MockKInstrumentation();
        instantiatior = new JvmMockKInstantiatior();
        proxyMaker = new JvmMockKProxyMaker(instantiatior, instrumentation);
        staticProxyMaker = new JvmMockKStaticProxyMaker(instrumentation);
    }

    @Override
    public void init(MockKAgentLogFactory logFactory) {
        JvmMockKProxyMaker.log = logFactory.logger(JvmMockKProxyMaker.class);
        MockKInstrumentationLoader.log = logFactory.logger(MockKInstrumentationLoader.class);
        MockKInstrumentation.log = logFactory.logger(MockKInstrumentation.class);
    }

    @Override
    public MockKInstantiatior getInstantiator() {
        return instantiatior;
    }

    @Override
    public MockKProxyMaker getProxyMaker() {
        return proxyMaker;
    }

    @Override
    public MockKStaticProxyMaker getStaticProxyMaker() {
        return staticProxyMaker;
    }
}
