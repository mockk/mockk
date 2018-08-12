package io.mockk.proxy

interface MockKAgentFactory {

    val instantiator: MockKInstantiatior

    val proxyMaker: MockKProxyMaker

    val staticProxyMaker: MockKStaticProxyMaker

    val constructorProxyMaker: MockKConstructorProxyMaker

    val interceptionScope: ProxyInterceptionScope

    fun init(logFactory: MockKAgentLogFactory)
}
