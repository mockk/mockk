package io.mockk.agent;

import javassist.ClassPool;
import javassist.Loader;

import static io.mockk.agent.MockKByteCodeTranslationRules.RULES;

public class MockKClassLoader extends Loader {
    private MockKClassLoader(ClassLoader parent, ClassPool cp) {
        super(parent, cp);
    }

    public static ClassLoader newClassLoader(ClassLoader parent) {
        ClassPool cp = new ClassPool(true);
        Loader loader = new MockKClassLoader(parent, cp);
        for (String ignore : RULES.getIgnoredPackagesAndClasses()) {
            loader.delegateLoadingOf(ignore);
        }
        try {
            loader.addTranslator(cp, new MockKTranslatorAdaptor(new MockKDefinalizer()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return loader;
    }
}
