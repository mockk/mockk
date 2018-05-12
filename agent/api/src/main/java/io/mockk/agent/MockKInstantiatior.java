package io.mockk.agent;

public interface MockKInstantiatior {
    <T> T instance(Class<T> cls);
}
