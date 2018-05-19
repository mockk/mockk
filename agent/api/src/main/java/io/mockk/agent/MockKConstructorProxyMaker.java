package io.mockk.agent;

public interface MockKConstructorProxyMaker {
    Cancelable<Class<?>> constructorProxy(
            Class<?> clazz,
            MockKInvocationHandler handler
    );
}
