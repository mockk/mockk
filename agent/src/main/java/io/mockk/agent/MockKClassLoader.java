package io.mockk.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;

import java.lang.reflect.Modifier;

import static io.mockk.agent.MockKByteCodeTranslationRules.RULES;

public class MockKClassLoader extends Loader {
    private MockKClassLoader(ClassLoader parent, ClassPool cp) {
        super(parent, cp);
    }

    public static ClassLoader newClassLoader(ClassLoader parent) {
        ClassPool cp = new ClassPool(true);
        Loader loader = new MockKClassLoader(parent, cp);
        for (String pkg : RULES.getIgnoredPackages()) {
            loader.delegateLoadingOf(pkg);
        }
        try {
            loader.addTranslator(cp, new MockKByteCodeTranslator());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return loader;
    }
}
