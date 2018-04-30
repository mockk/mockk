package io.mockk.agent;

public interface MockKStaticProxyMaker {
    void staticProxy(Class<?> clazz,
                     MockKInvocationHandler handler);

    void staticUnProxy(Class<?> clazz);
}
