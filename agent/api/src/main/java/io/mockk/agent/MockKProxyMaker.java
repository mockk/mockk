package io.mockk.agent;

public interface MockKProxyMaker {
    <T> T instance(Class<T> cls);

    <T> T proxy(
            Class<T> clazz,
            Class<?>[] interfaces,
            MockKInvocationHandler handler,
            boolean useDefaultConstructor,
            Object instance);

    void unproxy(Object instance);

    void staticProxy(Class<?> clazz,
                     MockKInvocationHandler handler);

    void staticUnProxy(Class<?> clazz);
}
