package io.mockk.proxy;

import io.mockk.agent.MockKAgentLogger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation.Context.Disabled.Factory;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;

import static java.util.Collections.synchronizedSet;
import static net.bytebuddy.dynamic.ClassFileLocator.Simple.of;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockKInstrumentation implements ClassFileTransformer {
    public static final MockKInstrumentation INSTANCE = new MockKInstrumentation();
    private Instrumentation instrumentation;
    private final MockKProxyAdvice advice;
    private final MockKStaticProxyAdvice staticAdvice;

    public static MockKAgentLogger log = MockKAgentLogger.NO_OP;

    private final Set<Class<?>> classesToTransform = synchronizedSet(new HashSet<Class<?>>());

    private ByteBuddy byteBuddy;


    MockKInstrumentation() {
        instrumentation = ByteBuddyAgent.install();

        if (!MockKInstrumentationLoader.INSTANCE.loadBootJar(instrumentation)) {
            log.trace("Failed to load mockk_boot.jar");
            instrumentation = null;
        } else {
            instrumentation.addTransformer(this, true);
        }

        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(Factory.INSTANCE);

        advice = new MockKProxyAdvice();
        staticAdvice = new MockKStaticProxyAdvice();

        MockKDispatcher.set(advice.getId(), advice);
        MockKDispatcher.set(staticAdvice.getId(), staticAdvice);
    }

    public boolean inject(List<Class<?>> classes) {
        if (instrumentation == null) {
            return false;
        }

        synchronized (classesToTransform) {
            classes.removeAll(classesToTransform);
            if (classes.isEmpty()) {
                return true;
            }

            log.trace("Injecting handler to " + classes);

            classesToTransform.addAll(classes);
        }


        Class[] cls = classes.toArray(new Class[classes.size()]);
        try {
            instrumentation.retransformClasses(cls);
            return true;
        } catch (UnmodifiableClassException e) {
            return false;
        }
    }

    public void enable() {
        instrumentation = ByteBuddyAgent.getInstrumentation();
    }

    public void disable() {
        instrumentation = null;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!classesToTransform.contains(classBeingRedefined)) {
            return null;
        }

        try {
            DynamicType.Unloaded<?> unloaded = byteBuddy.redefine(classBeingRedefined, of(classBeingRedefined.getName(), classfileBuffer))
                    .visit(Advice.withCustomMapping()
                            .bind(MockKProxyAdviceId.class, advice.getId())
                            .to(MockKProxyAdvice.class).on(
                                    isVirtual()
                                            .and(not(isDefaultFinalizer()))
                                            .and(not(isPackagePrivateJavaMethods()))))
                    .visit(Advice.withCustomMapping()
                            .bind(MockKProxyAdviceId.class, staticAdvice.getId())
                            .to(MockKStaticProxyAdvice.class).on(
                                    ElementMatchers.<MethodDescription>isStatic().and(not(isTypeInitializer())).and(not(isConstructor()))
                                            .and(not(isPackagePrivateJavaMethods()))))
                    .make();

            return unloaded.getBytes();
        } catch (Throwable e) {
            log.trace(e, "Failed to tranform class");
            return null;
        }
    }

    private static Junction<MethodDescription> isPackagePrivateJavaMethods() {
        return isDeclaredBy(nameStartsWith("java.")).and(isPackagePrivate());
    }

}
