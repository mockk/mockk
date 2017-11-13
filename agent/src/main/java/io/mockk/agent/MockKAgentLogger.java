package io.mockk.agent;

import net.bytebuddy.implementation.bytecode.Throw;

public interface MockKAgentLogger {
    MockKAgentLogger NO_OP = new MockKAgentLogger() {
        @Override
        public void debug(String msg) {
        }

        @Override
        public void trace(String msg) {
        }

        @Override
        public void trace(Throwable ex, String msg) {
        }
    };

    void debug(String msg);

    void trace(String msg);

    void trace(Throwable ex, String msg);
}
