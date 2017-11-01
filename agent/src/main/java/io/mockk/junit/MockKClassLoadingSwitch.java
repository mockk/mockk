package io.mockk.junit;

public class MockKClassLoadingSwitch {
    public static final boolean ON = !checkResourcePresent(
            MockKClassLoadingSwitch.class,
            "mockk-classloading-disabled.txt");

    private static boolean checkResourcePresent(Class<?> classBase,
                                                String resource) {

        return classBase.getResource(resource) != null;
    }
}
