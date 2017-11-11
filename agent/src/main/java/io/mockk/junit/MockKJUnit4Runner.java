package io.mockk.junit;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;

/**
 * Do class translation for JUnit4 tests.
 * <p>
 * Delegates actual running responsibility to one of runners:
 * <ul>
 * <li>runner annotated with @ChainedRunWith (first found in class hierarchy)</li>
 * <li>annotated with @RunWith for superclasses</li>
 * <li>JUnit4 default runner</li>
 * </ul>
 */
public class MockKJUnit4Runner extends Runner implements Filterable {
    public MockKJUnit4Runner(Class<?> cls) throws Exception {
    }

    @Override
    public Description getDescription() {
        return null;
    }

    @Override
    public void run(RunNotifier notifier) {

    }

    @Override
    public void filter(Filter filter) throws NoTestsRemainException {

    }
}
