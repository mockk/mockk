package io.mockk.testng;

import org.testng.IObjectFactory;

import java.lang.reflect.Constructor;

/**
 * Deprecated because inlining is implemented and
 * final classes and methods possible to mock through this technique.
 * So class loading transformation is not needed anymore.
 * <p>
 * Inlining is complex feature, so
 * please report any issues to:
 * https://github.com/oleksiyp/mockk/issues
 */
@Deprecated
public class MockKObjectFactory implements IObjectFactory {
    private void deprecated() {
        throw new RuntimeException(
                "Please remove MockKObjectFactory usage. " +
                        "Check JavaDoc for deprecation note.");
    }

    @Override
    public Object newInstance(Constructor constructor, Object... params) {
        deprecated();
        return null;
    }
}
