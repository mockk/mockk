package io.mockk.agent.inline;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockKInliner implements ClassFileTransformer {
    private static final Set<Class<?>> TO_TRANSFORM = synchronizedSet(newSetFromMap(new IdentityHashMap<Class<?>, Boolean>()));
    private static final Map<Class<?>, Boolean> TRANSFORMED_CLASSES = new IdentityHashMap<Class<?>, Boolean>();

    static {
        Instrumentation instrumentation = MockKHotAgent.getInstrumentation();
        if (instrumentation != null) {
            instrumentation.addTransformer(new MockKInliner(), true);
        }
    }

    private final ByteBuddy byteBuddy;
    private final MockKAdvice advice;

    public MockKInliner() {
        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(Implementation.Context.Disabled.Factory.INSTANCE);

        advice = new MockKAdvice();
        MockKDispatcher.set(advice.getId(), advice);
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {

        if (!TO_TRANSFORM.contains(classBeingRedefined)) {
            return null;
        }

        return byteBuddy.redefine(classBeingRedefined, ClassFileLocator.Simple.of(classBeingRedefined.getName(), classfileBuffer))
//                .visit(new ParameterWritingVisitorWrapper(classBeingRedefined))
                .visit(Advice.withCustomMapping()
                        .bind(MockKAdvice.Id.class, advice.getId())
                        .to(MockKAdvice.class).on(isVirtual()))
                .make()
                .getBytes();
    }

    public static boolean inject(Class<?> cls) {
        if (cls.isPrimitive()) {
            return false;
        }

        synchronized (TRANSFORMED_CLASSES) {
            Boolean transformationFlag = TRANSFORMED_CLASSES.get(cls);
            if (transformationFlag != null) {
                return transformationFlag;
            }

            boolean result = transformClassAndSuperclasses(cls);
            TRANSFORMED_CLASSES.put(cls, result);
            return result;
        }
    }

    private static boolean transformClassAndSuperclasses(Class<?> cls) {
        try {
            Instrumentation instrumentation = MockKHotAgent.getInstrumentation();
            if (instrumentation == null ||
                    !instrumentation.isRetransformClassesSupported()) {
                return false;
            }

            Set<Class<?>> superClasses = scanSuperClasses(cls);
            TO_TRANSFORM.addAll(superClasses);
            instrumentation.retransformClasses(superClasses.toArray(new Class<?>[0]));
            return true;
        } catch (UnmodifiableClassException e) {
            return false;
        }
    }

    private static Set<Class<?>> scanSuperClasses(Class<?> cls) {
        Set<Class<?>> superClasses = new HashSet<Class<?>>();

        while (cls != null) {
            superClasses.add(cls);
            cls = cls.getSuperclass();
        }
        return superClasses;
    }

    public static void registerHandler(Object instance, MockKMethodHandler handler) {
        MockKAdvice.registerHandler(instance, handler);
    }
}
