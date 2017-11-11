package io.mockk.agent.inline;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.Set;

public class MockKInliner {
    public static boolean inject(Class<?> cls) {
        if (cls.isPrimitive()) {
            return false;
        }

        Instrumentation instrumentation = MockKHotAgent.getInstrumentation();
        if (instrumentation == null) {
            return false;
        }

        synchronized (MockKDispatcher.TRANSFORMED_CLASSES) {
            Boolean transformationFlag = MockKDispatcher.TRANSFORMED_CLASSES.get(cls);
            if (transformationFlag != null) {
                return transformationFlag;
            }

            return transformClassAndSuperclasses(instrumentation, cls);
        }
    }

    private static boolean transformClassAndSuperclasses(Instrumentation instrumentation,
                                                         Class<?> cls) {

        Set<Class<?>> superClasses = scanSuperClasses(cls);
        if (superClasses.isEmpty()) {
            return true;
        }

        boolean result = true;
        try {
            MockKInlineTransformer.TO_TRANSFORM.addAll(superClasses);
            Class<?>[] superClassesArr = superClasses.toArray(new Class<?>[superClasses.size()]);
            instrumentation.retransformClasses(superClassesArr);
        } catch (UnmodifiableClassException e) {
            result = false;
        }

        for (Class<?> scls : superClasses) {
            MockKDispatcher.TRANSFORMED_CLASSES.put(scls, result);
        }
        return result;
    }

    private static Set<Class<?>> scanSuperClasses(Class<?> cls) {
        Set<Class<?>> superClasses = new HashSet<Class<?>>();

        while (cls != null) {
            Boolean transformationFlag = MockKDispatcher.TRANSFORMED_CLASSES.get(cls);
            if (transformationFlag == null) {
                superClasses.add(cls);
            }
            cls = cls.getSuperclass();
        }
        return superClasses;
    }

    public static void registerHandler(Object instance, MockKMethodHandler handler) {
        MockKAdvice.registerHandler(instance, handler);
    }
}
