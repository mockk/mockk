package io.mockk.proxy;

public class FinalClassFixture {

    public static int nestedRunCalls;

    public static final class Nested implements Runnable {
        public void run() {
            nestedRunCalls++;
        }
    }
}
