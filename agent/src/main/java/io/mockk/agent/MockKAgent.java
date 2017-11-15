package io.mockk.agent;

import net.bytebuddy.agent.Installer;

import java.lang.instrument.Instrumentation;

public class MockKAgent {
    public static void premain(String args, Instrumentation instrumentation) {
        Installer.premain(args, instrumentation);
    }
}

