package io.mockk.agent;

public class MockKAgentException extends RuntimeException {
    public MockKAgentException(String message) {
        super(message);
    }

    public MockKAgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
