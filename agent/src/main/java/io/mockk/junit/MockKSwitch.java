package io.mockk.junit;

public class MockKSwitch {
    public static final boolean CLASS_LOADING = !checkResourcePresent(
            MockKSwitch.class,
            "mockk-classloading-disabled.txt");

    public static final boolean INLINING = !checkResourcePresent(
            MockKSwitch.class,
            "mockk-inlining-disabled.txt");

    private static boolean checkResourcePresent(Class<?> classBase,
                                                String resource) {

        return classBase.getResource(resource) != null;
    }
}
