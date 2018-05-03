package io.mockk.proxy.jvm;

import io.mockk.agent.*;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation.Context.Disabled.Factory;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder;
import net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder.ParameterBinder.ForFixedValue.OfConstant;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher.Junction;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.Callable;

import static io.mockk.proxy.jvm.MockKInstrumentationLoader.LOADER;
import static java.lang.Thread.currentThread;
import static java.util.Collections.synchronizedMap;
import static java.util.Collections.synchronizedSet;
import static net.bytebuddy.dynamic.ClassFileLocator.Simple.of;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class MockKInstrumentation implements ClassFileTransformer {
    public static MockKAgentLogger log = MockKAgentLogger.NO_OP;

    private Map<Object, MockKInvocationHandler> handlers;
    private Map<Object, MockKInvocationHandler> staticHandlers;

    private volatile Instrumentation instrumentation;
    private MockKProxyInterceptor interceptor;
    private MockKProxyAdvice advice;
    private MockKStaticProxyAdvice staticAdvice;
    private MockKHashMapStaticProxyAdvice staticHashMapAdvice; // workaround #35

    private final Set<Class<?>> classesToTransform = synchronizedSet(new HashSet<Class<?>>());

    private static final Object BOOTSTRAP_MONITOR = new Object();

    private final TypeCache<CacheKey> proxyClassCache;

    private ByteBuddy byteBuddy;

    public MockKInstrumentation() {
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
            log.debug("Can't install ByteBuddy agent.\n" +
                    "Try running VM with MockK Java Agent\n" +
                    "i.e. with -javaagent:mockk-agent.jar option.");
        }


        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED);

        if (instrumentation != null) {
            class AdviceBuilder {
                void build() {
                    handlers = new JvmMockKWeakMap<Object, MockKInvocationHandler>();
                    advice = new MockKProxyAdvice(handlers);
                    interceptor = new MockKProxyInterceptor(handlers);
                    staticHandlers = new JvmMockKWeakMap<Object, MockKInvocationHandler>();
                    staticAdvice = new MockKStaticProxyAdvice(staticHandlers);
                    staticHashMapAdvice = new MockKHashMapStaticProxyAdvice(staticHandlers);

                    JvmMockKDispatcher.set(advice.getId(), advice);
                    JvmMockKDispatcher.set(staticAdvice.getId(), staticAdvice);
                    JvmMockKDispatcher.set(staticHashMapAdvice.getId(), staticHashMapAdvice);
                    JvmMockKDispatcher.set(interceptor.getId(), interceptor);
                }
            }
            new AdviceBuilder().build();
        } else {
            handlers = synchronizedMap(new IdentityHashMap<Object, MockKInvocationHandler>());
            staticHandlers = synchronizedMap(new IdentityHashMap<Object, MockKInvocationHandler>());
        }

        proxyClassCache = new TypeCache<CacheKey>(TypeCache.Sort.WEAK);
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

            log.trace("Injecting handle to " + classes);

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


    public <T> Class<?> subclass(final Class<T> clazz, final Class<?>[] interfaces) {
        CacheKey key = new CacheKey(clazz, interfaces);
        final ClassLoader classLoader = clazz.getClassLoader();
        Object monitor = classLoader == null ? BOOTSTRAP_MONITOR : classLoader;
        return proxyClassCache.findOrInsert(classLoader, key,
                new Callable<Class<?>>() {
                    @Override
                    public Class<?> call() {
                        ClassLoader classLoader = new MultipleParentClassLoader.Builder()
                                .append(clazz)
                                .append(interfaces)
                                .append(currentThread().getContextClassLoader())
                                .append(MockKProxyInterceptor.class)
                                .build(MockKProxyInterceptor.class.getClassLoader());


                        return byteBuddy.subclass(clazz)
                                .implement(interfaces)
                                .method(any())
                                .intercept(MethodDelegation.withDefaultConfiguration()
                                        .withBinders(OfConstant.of(MockKProxyAdviceId.class, interceptor.getId()))
                                        .to(MockKProxyInterceptor.class))
                                .make()
                                .load(classLoader, ClassLoadingStrategy.Default.INJECTION)
                                .getLoaded();
                    }
                }, monitor);
    }


    private long staticProxyAdviceId(String className) {
        // workaround #35
        return className.equals("java/util/HashMap") ? staticHashMapAdvice.getId() : staticAdvice.getId();
    }

    private Class<? extends JvmMockKProxyDispatcher> staticProxyAdvice(String className) {
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
