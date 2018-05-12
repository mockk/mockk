package io.mockk.agent;

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

        @Override
        public void warn(Throwable ex, String msg) {

        }
    };

    void debug(String msg);

    void trace(String msg);

    void trace(Throwable ex, String msg);

    void warn(Throwable ex, String msg);
}
