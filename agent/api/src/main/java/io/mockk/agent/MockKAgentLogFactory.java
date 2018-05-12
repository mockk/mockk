package io.mockk.agent;

public interface MockKAgentLogFactory {
    MockKAgentLogger logger(Class<?> cls);
}
