package io.mockk.agent;

public interface Cancelable<T> {
    T get();

    void cancel();
}
