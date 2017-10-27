package io.mockk.junit;

import io.mockk.agent.MockKClassLoader;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;

import java.lang.reflect.Constructor;

/**
 * Do class translation for JUnit4 tests.
 * <p>
 * Delegates actual running responsibility to one of runners:
 * <li>
 * <ul>runner annotated with @ChainedRunWith (first found in class hierarchy)</ul>
 * <ul>annotated with @RunWith for superclasses</ul>
 * <ul>JUnit4 default runner</ul>
 * </li>
 */
public class MockKJUnit4Runner extends Runner {
    private final Runner runner;

    public MockKJUnit4Runner(Class<?> cls) throws Exception {
        ClassLoader loader = MockKClassLoader.newClassLoader(cls.getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        cls = loader.loadClass(cls.getName());

        Class<?> runnerClass = findChainedRunner(cls);
        if (runnerClass == null) {
            runnerClass = findRunnerInSuperclass(cls);
        }
        if (runnerClass == null) {
            runnerClass = JUnit4.class;
        }

        Constructor<?> constructor = runnerClass.getConstructor(Class.class);
        this.runner = (Runner) constructor.newInstance(cls);
    }

    private Class<?> findChainedRunner(Class<?> cls) {
        while (cls != null) {
            ChainedRunWith chainedRunWith = cls.getAnnotation(ChainedRunWith.class);
            if (chainedRunWith != null) {
                return chainedRunWith.value();
            }
            cls = cls.getSuperclass();
        }
        return null;
    }


    private Class<?> findRunnerInSuperclass(Class<?> cls) {
        cls = cls.getSuperclass();
        while (cls != null) {
            RunWith runWith = cls.getAnnotation(RunWith.class);
            if (runWith != null) {
                return runWith.value();
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    @Override
    public Description getDescription() {
        return runner.getDescription();
    }

    @Override
    public void run(RunNotifier notifier) {
        runner.run(notifier);
    }
}
