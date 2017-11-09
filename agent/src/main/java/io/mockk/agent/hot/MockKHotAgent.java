package io.mockk.agent.hot;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.lang.instrument.Instrumentation;

public class MockKHotAgent {
    static {
        try {
            ByteBuddyAgent.install();
        } catch (IllegalStateException ex) {
            // skip
        }

    }

    public static Instrumentation getInstrumentation() {
        return ByteBuddyAgent.getInstrumentation();
    }
}
