package io.mockk.agent;

import javassist.ClassPool;
import javassist.CtClass;

public interface JavassistTranslator {
    void start(ClassPool pool);

    void onLoad(CtClass cls);
}
