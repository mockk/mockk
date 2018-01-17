package io.mockk.junit;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

/**
 * Deprecated because inlining is implemented and
 * final classes and methods possible to mock through this technique.
 * So class loading transformation is not needed anymore.
 * <p>
 * Inlining is complex feature, so
 * please report any issues to:
 * https://github.com/oleksiyp/mockk/issues
 */
public class MockKJUnit4Runner extends Runner {
    private void deprecated() {
        throw new RuntimeException(
                "Please remove @RunWith(MockKJUnit4Runner.class) annotation. " +
                        "Check JavaDoc for deprecation note.");
    }

    public MockKJUnit4Runner(Class<?> cls) throws Exception {
    }

    @Override
    public Description getDescription() {
        deprecated();
        return null;
    }


    @Override
    public void run(RunNotifier notifier) {
        deprecated();
    }
}
