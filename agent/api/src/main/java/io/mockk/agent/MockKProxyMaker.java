package io.mockk.agent;

public interface MockKProxyMaker {
    <T> T proxy(
            Class<T> clazz,
            Class<?>[] interfaces,
            MockKInvocationHandler handler,
            boolean useDefaultConstructor,
            Object instance);

    void unproxy(Object instance);
}
