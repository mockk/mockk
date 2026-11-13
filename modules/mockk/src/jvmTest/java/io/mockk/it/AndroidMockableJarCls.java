package io.mockk.it;

/**
 * Simulates a class of the mockable android.jar that the Android Gradle Plugin generates for unit
 * tests: when {@code testOptions.unitTests.isReturnDefaultValues} is false, every method body is
 * replaced with a RuntimeException thrower (issue #1250, android.graphics.Color#argb).
 */
public class AndroidMockableJarCls {
    private AndroidMockableJarCls() {
    }

    /** Mirrors {@code android.graphics.Color.argb(int alpha, int red, int green, int blue)}. */
    public static int argb(int alpha, int red, int green, int blue) {
        throw new RuntimeException(
            "Method argb in android.graphics.Color not mocked. "
                + "See https://developer.android.com/r/studio-ui/build/not-mocked for details.");
    }

    /** Mirrors {@code android.graphics.Color.argb(float red, float green, float blue, float alpha)}. */
    public static int argb(float red, float green, float blue, float alpha) {
        throw new RuntimeException(
            "Method argb in android.graphics.Color not mocked. "
                + "See https://developer.android.com/r/studio-ui/build/not-mocked for details.");
    }

    /** A body that fails the way real code can, not like a mockable-jar stub. */
    public static int parseInt(String value) {
        throw new IllegalStateException("not a number");
    }
}
