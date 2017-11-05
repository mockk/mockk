package io.mockk.testng;

import io.mockk.junit.MockKClassLoaderUtil;
import org.testng.IObjectFactory;

import java.lang.reflect.Constructor;

public class MockKObjectFactory implements IObjectFactory {
    @Override
    public Object newInstance(Constructor constructor, Object... params) {
        try {
            return MockKClassLoaderUtil.getConstructor(constructor).newInstance(params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
