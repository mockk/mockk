package io.mockk.proxy;

import io.mockk.agent.MockKAgentException;
import io.mockk.agent.MockKAgentLogger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.MultipleParentClassLoader;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher.Junction;
import net.bytebuddy.matcher.ElementMatchers;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static net.bytebuddy.implementation.MethodDelegation.to;

public class MockKProxyMaker {
    public static final MockKProxyMaker INSTANCE = new MockKProxyMaker();

    private final ByteBuddy byteBuddy;

    public static MockKAgentLogger log = MockKAgentLogger.NO_OP;

    private final ObjenesisStd objenesis;

    private TypeCache<CacheKey> proxyClassCache;

    private TypeCache<CacheKey> instanceProxyClassCache;

    private MockKWeakConcurrentHashMap<Class<?>, ObjectInstantiator<?>> instantiators = new MockKWeakConcurrentHashMap<Class<?>, ObjectInstantiator<?>>();

    private static final Set<Class<?>> EXCLUDES = new HashSet<Class<?>>(Arrays.asList(Class.class,
            Boolean.class,
            Byte.class,
            Short.class,
            Character.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class));


    public MockKProxyMaker() {
        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED);
        objenesis = new ObjenesisStd(true);
        proxyClassCache = new TypeCache<CacheKey>(TypeCache.Sort.SOFT);
        instanceProxyClassCache = new TypeCache<CacheKey>(TypeCache.Sort.SOFT);
    }

    public <T> T instance(Class<T> cls) {
        if (Modifier.isFinal(cls.getModifiers())) {
            return newEmptyInstance(cls);
        }

        try {
            T instance = instantiateViaProxy(cls);
            if (instance != null) {
                return instance;
            }
        } catch (Exception ex) {
            log.trace(ex, "Failed to instantiate via proxy " + cls + ". " +
                    "Doing just instantiation");
        }
        return newEmptyInstance(cls);
    }

    private <T> T instantiateViaProxy(final Class<T> cls) {
        log.trace("Instantiating " + cls + " via subclass proxy");

        Class<?> proxyCls =
                instanceProxyClassCache.findOrInsert(cls.getClassLoader(),
                        new CacheKey(cls, new Class[0]),
                        new Callable<Class<?>>() {
                            @Override
                            public Class<?> call() throws Exception {
                                return byteBuddy.subclass(cls)
                                        .make()
                                        .load(cls.getClassLoader())
                                        .getLoaded();
                            }
                        });

        return cls.cast(newEmptyInstance(proxyCls));
    }


    public <T> T proxy(
            final Class<T> clazz,
            final Class<?>[] interfaces,
            MockKInvocationHandler handler,
            boolean useDefaultConstructor) {

        boolean transformed = canInject(clazz) && MockKInstrumentation.INSTANCE.inject(getAllSuperclasses(clazz));

        Class<?> proxyClass;
        if (!Modifier.isFinal(clazz.getModifiers())) {
            log.trace("Building subclass proxy for " + clazz +
                    " with additional interfaces " + asList(interfaces));
            CacheKey key = new CacheKey(clazz, interfaces);
            final ClassLoader classLoader = clazz.getClassLoader();
            proxyClass = proxyClassCache.findOrInsert(classLoader, key,
                    new Callable<Class<?>>() {
                        @Override
                        public Class<?> call() throws Exception {
                            ClassLoader classLoader = new MultipleParentClassLoader.Builder()
                                    .append(clazz)
                                    .append(interfaces)
                                    .append(currentThread().getContextClassLoader())
                                    .append(MockKProxyInterceptor.class)
                                    .build(MockKProxyInterceptor.class.getClassLoader());


                            return byteBuddy.subclass(clazz)
                                    .implement(interfaces)
                                    .method(any())
                                    .intercept(to(MockKProxyInterceptor.class))
                                    .make()
                                    .load(classLoader)
                                    .getLoaded();
                        }
                    });

            if (!transformed) {
                warnOnFinalMethods(clazz);
            }
        } else {

            if (!transformed) {
                if (clazz.isPrimitive()) {
                    throw new MockKAgentException("Failed to create proxy for " + clazz + ".\n" +
                            clazz + " is a primitive");
                } else if (clazz.isArray()) {
                    throw new MockKAgentException("Failed to create proxy for " + clazz + ".\n" +
                            clazz + " is an array");
                } else if (!canInject(clazz)) {
                    throw new MockKAgentException("Failed to create proxy for " + clazz + ".\n" +
                            clazz + " is one of excluded classes");
                } else {
                    throw new MockKAgentException("Failed to create proxy for " + clazz + ".\n" +
                            "Instrumentation is not available and class is final.\n" +
                            "Add -javaagent option to enabled MockK Java Agent at JVM startup");
                }
            }
            if (interfaces.length != 0) {
                throw new MockKAgentException("Failed to create proxy for " + clazz + ".\n" +
                        "More interfaces requested and class is final.");
            }

            log.trace("Taking instance of " + clazz + " itself because it is final.");

            proxyClass = clazz;
        }

        try {
            if (useDefaultConstructor) {
                log.trace("Instantiating proxy for " + clazz + " via default constructor.");
            } else {
                log.trace("Instantiating proxy for " + clazz + " via objenesis.");
            }
            T instance = clazz.cast(useDefaultConstructor
                    ? proxyClass.newInstance()
                    : newEmptyInstance(proxyClass));

            MockKInvocationHandler.HANDLERS.put(instance, handler);

            return instance;
        } catch (Exception e) {
            throw new MockKAgentException("Instantiation exception", e);
        }
    }

    private <T> boolean canInject(Class<T> clazz) {
        return !EXCLUDES.contains(clazz);
    }

    private void warnOnFinalMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<Method>();
        while (clazz != null && !clazz.getName().equals(Object.class.getName())) {
            methods.addAll(asList(clazz.getDeclaredMethods()));
            clazz = clazz.getSuperclass();
        }

        for (Method method : methods) {
            int modifiers = method.getModifiers();
            if (!Modifier.isPrivate(modifiers) && Modifier.isFinal(modifiers)) {
                log.debug("It is impossible to intercept calls to " + method + " for " +
                        method.getDeclaringClass() + " because it is final");
            }
        }
    }


    private <T> T newEmptyInstance(Class<T> clazz) {
        ObjectInstantiator<?> inst = instantiators.get(clazz);
        if (inst == null) {
            inst = objenesis.getInstantiatorOf(clazz);
            instantiators.put(clazz, inst);
        }
        return clazz.cast(inst.newInstance());
    }

    public void staticProxy(Class<?> clazz,
                            MockKInvocationHandler handler) {
        log.debug("Injecting handler to " + clazz + " for static methods");

        ArrayList<Class<?>> lst = new ArrayList<Class<?>>();
        lst.add(clazz);
        boolean transformed = MockKInstrumentation.INSTANCE.inject(lst);
        if (!transformed) {
            throw new MockKAgentException("Failed to create proxy for " + clazz + ".\n" +
                    "Add MockK Java Agent instrumentation.");
        }

        MockKInvocationHandler.HANDLERS.put(clazz, handler);
    }

    public void staticUnProxy(Class<?> clazz) {
        MockKInvocationHandler.HANDLERS.remove(clazz);
    }

    private static Junction<MethodDescription> any() {
        return ElementMatchers.any();
    }

    private List<Class<?>> getAllSuperclasses(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<Class<?>>();

        while (clazz != null) {
            result.add(clazz);
            addInterfaces(result, clazz);
            clazz = clazz.getSuperclass();
        }

        return new ArrayList<Class<?>>(result);
    }

    private void addInterfaces(Set<Class<?>> result, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        for (Class<?> intf : clazz.getInterfaces()) {
            result.add(intf);
            addInterfaces(result, intf.getSuperclass());
        }
    }

    private static final class CacheKey {
        private final Class<?> clazz;
        private final Set<Class<?>> interfaces;

        public CacheKey(Class<?> clazz, Class<?>[] interfaces) {
            this.clazz = clazz;
            this.interfaces = new HashSet<Class<?>>(asList(interfaces));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            return clazz.equals(cacheKey.clazz) &&
                    interfaces.equals(cacheKey.interfaces);
        }

        @Override
        public int hashCode() {
            return 31 * clazz.hashCode() + interfaces.hashCode();
        }
    }
}
