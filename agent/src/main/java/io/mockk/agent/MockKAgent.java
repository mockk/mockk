package io.mockk.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Modifier;

import static io.mockk.agent.MockKByteCodeTranslationRules.RULES;

public class MockKAgent {
    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new MockKClassFileTransformer());
    }

}

