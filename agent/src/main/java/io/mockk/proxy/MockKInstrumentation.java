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

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;

import static io.mockk.proxy.MockKInstrumentationLoader.LOADER;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;
import static net.bytebuddy.dynamic.ClassFileLocator.Simple.of;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockKInstrumentation implements ClassFileTransformer {
    public static MockKAgentLogger log = MockKAgentLogger.NO_OP;

    public static MockKInstrumentation INSTANCE;
    private Map<Object, MockKInvocationHandler> handlers;
    private Map<Object, MockKInvocationHandler> staticHandlers;

    private volatile Instrumentation instrumentation;
    private MockKProxyAdvice advice;
    private MockKStaticProxyAdvice staticAdvice;
    private MockKHashMapStaticProxyAdvice staticHashMapAdvice; // workaround #35

    private final Set<Class<?>> classesToTransform = synchronizedSet(new HashSet<Class<?>>());

    private ByteBuddy byteBuddy;

    public static void init() {
        INSTANCE = new MockKInstrumentation();
    }

    MockKInstrumentation() {
        instrumentation = ByteBuddyAgent.install();

        if (instrumentation != null) {
            log.trace("Byte buddy agent installed");
            if (LOADER.loadBootJar(instrumentation)) {
                log.trace("Installing MockKInstrumentation transformer");
                instrumentation.addTransformer(this, true);
            } else {
                log.trace("Can't inject boot jar.");
                instrumentation = null;
            }
        } else {
            log.trace("Can't install ByteBuddy agent.\n" +
                    "Try running VM with MockK Java Agent\n" +
                    "i.e. with -javaagent:mockk-agent.jar option.");
        }


        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .with(Factory.INSTANCE);

        if (instrumentation != null) {
            class AdviceBuilder {
                void build() {
                    handlers = new MockKWeakMap<Object, MockKInvocationHandler>();
                    advice = new MockKProxyAdvice(handlers);
                    staticHandlers = new MockKWeakMap<Object, MockKInvocationHandler>();
                    staticAdvice = new MockKStaticProxyAdvice(staticHandlers);
                    staticHashMapAdvice = new MockKHashMapStaticProxyAdvice(staticHandlers);

                    MockKDispatcher.set(advice.getId(), advice);
                    MockKDispatcher.set(staticAdvice.getId(), staticAdvice);
                    MockKDispatcher.set(staticHashMapAdvice.getId(), staticHashMapAdvice);
                }
            }
            new AdviceBuilder().build();
        } else {
            handlers = synchronizedMap(new IdentityHashMap<Object, MockKInvocationHandler>());
            staticHandlers = synchronizedMap(new IdentityHashMap<Object, MockKInvocationHandler>());
        }
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
            log.trace("Injected OK");
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
                                    isMethod()
                                            .and(not(isStatic()))
                                            .and(not(isDefaultFinalizer()))))
                    .visit(Advice.withCustomMapping()
                            .bind(MockKProxyAdviceId.class, staticProxyAdviceId(className))
                            .to(staticProxyAdvice(className)).on(
                                    isStatic()
                                            .and(not(isTypeInitializer()))
                                            .and(not(isConstructor()))
                            ))
                    .make();

            return unloaded.getBytes();
        } catch (Throwable e) {
            log.warn(e, "Failed to transform class");
            return null;
        }
    }

    private long staticProxyAdviceId(String className) {
        // workaround #35
        return className.equals("java/util/HashMap") ? staticHashMapAdvice.getId() : staticAdvice.getId();
    }

    private Class<? extends MockKProxyDispatcher> staticProxyAdvice(String className) {
        // workaround #35
        return className.equals("java/util/HashMap") ? MockKHashMapStaticProxyAdvice.class : MockKStaticProxyAdvice.class;
    }

    private static Junction<MethodDescription> isPackagePrivateJavaMethods() {
        return isDeclaredBy(nameStartsWith("java.")).and(isPackagePrivate());
    }

    public <T> void hook(T instance, MockKInvocationHandler handler) {
        handlers.put(instance, handler);
    }

    public <T> void unhook(T instance) {
        handlers.remove(instance);
    }

    public void hookStatic(Class<?> clazz, MockKInvocationHandler handler) {
        staticHandlers.put(clazz, handler);
    }

    public void unhookStatic(Class<?> clazz) {
        staticHandlers.remove(clazz);
    }

    public MockKInvocationHandler getHook(Object self) {
        return handlers.get(self);
    }
}
