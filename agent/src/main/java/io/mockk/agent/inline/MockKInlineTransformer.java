package io.mockk.agent.inline;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.synchronizedSet;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockKInlineTransformer implements ClassFileTransformer {
    public static final Set<Class<?>> TO_TRANSFORM = synchronizedSet(newSetFromMap(new HashMap<Class<?>, Boolean>()));
    private final ByteBuddy byteBuddy;

    private final MockKAdvice advice;

    public MockKInlineTransformer(Instrumentation instrumentation) {
        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(Implementation.Context.Disabled.Factory.INSTANCE);
        advice = new MockKAdvice();
        MockKDispatcher.set(advice.getId(), advice);
        instrumentation.addTransformer(this, true);
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
                .visit(Advice.withCustomMapping()
                        .bind(MockKAdvice.Id.class, advice.getId())
                        .to(MockKAdvice.class).on(isVirtual()
                                .and(not(isBridge().or(isDefaultFinalizer())))
                                .and(not(isDeclaredBy(nameStartsWith("java.")).<MethodDescription>and(isPackagePrivate())))))
                .make()
                .getBytes();
    }
}
