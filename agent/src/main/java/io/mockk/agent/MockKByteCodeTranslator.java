package io.mockk.agent;

import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.InnerClassesAttribute;

import java.lang.reflect.Method;


public class MockKByteCodeTranslator implements Translator {
    private Method checkModify;

    public void start(ClassPool pool) {
        try {
            checkModify = CtClass.class.getDeclaredMethod("checkModify");
            checkModify.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onLoad(ClassPool pool, String classname) throws NotFoundException, CannotCompileException {
        CtClass cls = pool.get(classname);
        onLoad(cls);
    }

    public void onLoad(CtClass cls) {
        removeFinal(cls);
    }

    private void removeFinal(CtClass cls) {
        removeFinalOnClass(cls);
        removeFinalOnMethods(cls);
    }

    private void removeFinalOnMethods(CtClass cls) {
        for (CtMethod method : cls.getDeclaredMethods()) {
            if (Modifier.isFinal(method.getModifiers())) {
                method.setModifiers(Modifier.clear(method.getModifiers(), Modifier.FINAL));
            }
        }
    }


    private void removeFinalOnClass(CtClass clazz) {
        int modifiers = clazz.getModifiers();
        ClassFile classFile = clazz.getClassFile2();

        if (Modifier.isFinal(modifiers)) {
            try {
                checkModify.invoke(clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            classFile.setAccessFlags(AccessFlag.of(Modifier.clear(modifiers, Modifier.FINAL)));

            AttributeInfo ai = classFile.getAttribute(InnerClassesAttribute.tag);
            if (ai != null && ai instanceof InnerClassesAttribute) {
                InnerClassesAttribute ica = (InnerClassesAttribute) ai;
                for (int i = 0; i < ica.tableLength(); i++) {
                    if (classFile.getName().equals(ica.innerClass(i))) {
                        int accessFlags = ica.accessFlags(i);
                        ica.setAccessFlags(i, AccessFlag.of(
                                Modifier.clear(accessFlags, Modifier.FINAL)));
                    }
                }
            }
        }
    }
}
