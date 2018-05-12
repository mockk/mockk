package io.mockk.proxy.android;

import android.os.Build;
import io.mockk.agent.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class AndroidMockKAgentFactory implements MockKAgentFactory {
    private static final String DISPATCHER_CLASS_NAME =
            "io.mockk.proxy.android.AndroidMockKDispatcher";
    private static final String DISPATCHER_JAR = "dispatcher.jar";

    private final AndroidMockKInstantiator instantiator;
    private final AndroidMockKProxyMaker proxyMaker;
    private final AndroidMockKStaticProxyMaker staticProxyMaker;

    /**
     * {@link AndroidMockKJvmtiAgent} set up during one time init
     */
    private static final AndroidMockKJvmtiAgent AGENT;

    /**
     * Error  during one time init or {@code null} if init was successful
     */
    private static final Throwable INITIALIZATION_ERROR;

    /**
     * Class injected into the bootstrap classloader. All entry hooks added to methods will call
     * this class.
     */
    public static final Class DISPATCHER_CLASS;

    static {
        AndroidMockKJvmtiAgent agent;
        Throwable initializationError = null;
        Class dispatcherClass = null;
        try {
            try {
                agent = new AndroidMockKJvmtiAgent();

                URL disptacherJar = AndroidMockKProxyMaker.class.getClassLoader().getResource(DISPATCHER_JAR);
                if (disptacherJar == null) {
                    throw new RuntimeException("'" + DISPATCHER_JAR + "' not found");
                }
                try (InputStream is = disptacherJar.openStream()) {
                    agent.appendToBootstrapClassLoaderSearch(is);
                }

                try {
                    dispatcherClass = Class.forName(DISPATCHER_CLASS_NAME, true,
                            Object.class.getClassLoader());

                    if (dispatcherClass == null) {
                        throw new IllegalStateException(DISPATCHER_CLASS_NAME
                                + " could not be loaded");
                    }
                } catch (ClassNotFoundException cnfe) {
                    throw new IllegalStateException(
                            "MockK failed to inject the AndroidMockKDispatcher class into the "
                                    + "bootstrap class loader\n\nIt seems like your current VM does not "
                                    + "support the jvmti API correctly.", cnfe);
                }
            } catch (IOException ioe) {
                throw new IllegalStateException(
                        "MockK could not self-attach a jvmti agent to the current VM. This "
                                + "feature is required for inline mocking.\nThis error occured due to an "
                                + "I/O error during the creation of this agent: " + ioe + "\n\n"
                                + "Potentially, the current VM does not support the jvmti API correctly",
                        ioe);
            }
        } catch (Throwable throwable) {
            agent = null;
            initializationError = throwable;
        }

        AGENT = agent;
        INITIALIZATION_ERROR = initializationError;
        DISPATCHER_CLASS = dispatcherClass;
    }


    public AndroidMockKAgentFactory() {
        if (INITIALIZATION_ERROR != null) {
            throw new RuntimeException(
                    "Could not initialize inline mock maker.\n"
                            + "\n"
                            + "Release: Android " + Build.VERSION.RELEASE + " " + Build.VERSION.INCREMENTAL
                            + "Device: " + Build.BRAND + " " + Build.MODEL, INITIALIZATION_ERROR);
        }


        Map<Object, MockKInvocationHandlerAdapter> mocks = new AndroidMockKMap();

        AndroidMockKMethodAdvice advice = new AndroidMockKMethodAdvice(mocks);
        AndroidMockKClassTransformer classTransformer = new AndroidMockKClassTransformer(
                AGENT,
                DISPATCHER_CLASS,
                advice
        );

        instantiator = new AndroidMockKInstantiator();
        proxyMaker = new AndroidMockKProxyMaker(instantiator, classTransformer, mocks);
        staticProxyMaker = new AndroidMockKStaticProxyMaker(classTransformer, mocks);
    }

    @Override
    public void init(MockKAgentLogFactory logFactory) {
        AndroidMockKInstantiator.log = logFactory.logger(AndroidMockKInstantiator.class);
    }

    @Override
    public MockKInstantiatior getInstantiator() {
        return instantiator;
    }

    @Override
    public MockKProxyMaker getProxyMaker() {
        return proxyMaker;
    }

    @Override
    public MockKStaticProxyMaker getStaticProxyMaker() {
        return staticProxyMaker;
    }
}
