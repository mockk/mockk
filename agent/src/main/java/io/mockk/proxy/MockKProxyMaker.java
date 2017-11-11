package io.mockk.proxy;

public class MockKProxyMaker {
    public <T> T instance(Class<T> clazz) {
        return null;
    }

    public <T> T proxy(
            String name,
            Class<T> superclass,
            Class<?>[] interfaces,
            MockKInvocationHandler handler) {

        // how to approach
        // 1. if class is not final then subclass, proxy all overridable methods
        // 1a. if method is not overridable instrument in class first declared final
        // 2. if class is final then instrument all methods

        return null;
    }

    public void staticProxy(Class<?> clazz,
                            MockKInvocationHandler handler) {

    }
}
