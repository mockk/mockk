package io.mockk.agent;

import java.lang.instrument.Instrumentation;

public class MockKAgent {
    public static boolean running = false;

    public static void premain(String args, Instrumentation instrumentation) {
        running = true;
        instrumentation.addTransformer(new MockKJavassistTransformer(new MockKDefinalizer()));
    }

}

