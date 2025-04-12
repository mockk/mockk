package io.mockk.core;

public class ClassWithStaticField {
    private static final ClassWithStaticField INSTANCE = new ClassWithStaticField();
    public static ClassWithStaticField instance() {
        return INSTANCE;
    }
    public int foo() { return 10; }
}
