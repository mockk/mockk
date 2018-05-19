package io.mockk.agent;

public interface MockKAgentFactory {
    void init(MockKAgentLogFactory logFactory);

    MockKInstantiatior getInstantiator();

    MockKProxyMaker getProxyMaker();

    MockKStaticProxyMaker getStaticProxyMaker();

    MockKConstructorProxyMaker getConstructorProxyMaker();
}
