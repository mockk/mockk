package io.mockk.agent;

public interface MockKStaticProxyMaker {
    Cancelable<Class<?>> staticProxy(
            Class<?> clazz,
            MockKInvocationHandler handler
    );
}
