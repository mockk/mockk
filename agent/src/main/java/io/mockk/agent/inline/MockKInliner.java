package io.mockk.agent.inline;

import io.mockk.agent.hot.MockKHotAgent;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatcher.Junction;

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
    public static final Junction<MethodDescription> JUNCTION =
            isVirtual()
                    .and(not(isBridge().or(isHashCode()).or(isEquals()).or(isDefaultFinalizer())))
                    .and(not(isDeclaredBy(nameStartsWith("java.")).<MethodDescription>and(isPackagePrivate())));

    static {
        Instrumentation instrumentation = MockKHotAgent.getInstrumentation();
        if (instrumentation != null) {
            instrumentation.addTransformer(new MockKInliner(), true);
        }
    }

    private final ByteBuddy byteBuddy;

    public MockKInliner() {
        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(Implementation.Context.Disabled.Factory.INSTANCE);
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
                        .to(MockKAdvice.class)
                        .on(JUNCTION))
                .visit(Advice.withCustomMapping()
                        .to(MockKAdvice.HashCode.class).on(isHashCode()))
                .visit(Advice.withCustomMapping()
                        .to(MockKAdvice.Equals.class).on(isEquals()))
                .make()
                .getBytes();
    }

    private static final Set<Class<?>> TO_TRANSFORM = synchronizedSet(newSetFromMap(new IdentityHashMap<Class<?>, Boolean>()));
    private static final Map<Class<?>, Boolean> TRANSFORMED_CLASSES = new IdentityHashMap<Class<?>, Boolean>();

    public static boolean inject(Class<?> cls) {
        synchronized (TRANSFORMED_CLASSES) {
            Boolean transformationFlag = TRANSFORMED_CLASSES.get(cls);
            if (transformationFlag != null) {
                return transformationFlag;
            }

            boolean result = false;
            try {
                Instrumentation instrumentation = MockKHotAgent.getInstrumentation();
                if (instrumentation != null) {
                    TO_TRANSFORM.add(cls);
                    instrumentation.retransformClasses(cls);
                    result = true;
                }
            } catch (UnmodifiableClassException e) {
                // skip
            }
            TRANSFORMED_CLASSES.put(cls, result);
            return result;
        }
    }
}
