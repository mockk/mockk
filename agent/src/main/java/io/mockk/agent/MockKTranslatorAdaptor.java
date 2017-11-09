package io.mockk.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.Translator;

public class MockKTranslatorAdaptor implements Translator {
    private JavassistTranslator translator;

    public MockKTranslatorAdaptor(JavassistTranslator translator) {
        this.translator = translator;
    }

    @Override
    public void start(ClassPool pool) throws NotFoundException, CannotCompileException {
        translator.start(pool);
    }

    @Override
    public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        translator.onLoad(pool.get(classname));
    }
}
