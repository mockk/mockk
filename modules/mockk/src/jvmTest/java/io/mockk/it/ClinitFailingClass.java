package io.mockk.it;

public class ClinitFailingClass {
    static {
        if (true) {
            throw new RuntimeException("simulated failing <clinit>, see #1538");
        }
    }

    public String value() {
        return "real value";
    }
}
