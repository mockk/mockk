package io.mockk.proxy.android;

import io.mockk.agent.*;

public class AndroidMockKAgentFactory implements MockKAgentFactory {

    private final AndroidMockKInstantiator instantiator;
    private final AndroidMockKProxyMaker proxyMaker;
    private final AndroidMockKStaticProxyMaker staticProxyMaker;

    public AndroidMockKAgentFactory() {
        instantiator = new AndroidMockKInstantiator();
        proxyMaker = new AndroidMockKProxyMaker(instantiator);
        staticProxyMaker = new AndroidMockKStaticProxyMaker();
    }

    @Override
    public void init(MockKAgentLogFactory logFactory) {

    }

    @Override
    public MockKInstantiatior getInstantiator() {
        return instantiator;
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
