package io.mockk.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import static io.mockk.agent.MockKByteCodeTranslationRules.RULES;

class MockKClassFileTransformer implements ClassFileTransformer {
    private final ClassPool pool;
    private final MockKByteCodeTranslator translator;

    public MockKClassFileTransformer() {
        pool = new ClassPool();
        translator = new MockKByteCodeTranslator();
        translator.start(pool);
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (RULES.isIgnored(className)) {
            return classfileBuffer;
        }

        ByteArrayInputStream in = new ByteArrayInputStream(classfileBuffer);
        try {
            CtClass cls = pool.makeClass(in);
            translator.onLoad(cls);
            cls.detach();
            return cls.toBytecode();
        } catch (IOException e) {
            return classfileBuffer;
        } catch (CannotCompileException e) {
            System.err.println("MockK compilation error: " + e.getReason());
            return classfileBuffer;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // skip
            }
        }
    }

}
