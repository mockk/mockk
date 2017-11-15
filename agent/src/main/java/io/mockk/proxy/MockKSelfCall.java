package io.mockk.proxy;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

public class MockKSelfCall {
    public static final ThreadLocal<Object> SELF_CALL = new ThreadLocal<Object>();


}
