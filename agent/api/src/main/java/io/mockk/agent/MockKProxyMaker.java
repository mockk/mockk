package io.mockk.agent;

public interface MockKProxyMaker {
    <T> Cancelable<T> proxy(
            Class<T> clazz,
            Class<?>[] interfaces,
            MockKInvocationHandler handler,
            boolean useDefaultConstructor,
            Object instance
    );
}
