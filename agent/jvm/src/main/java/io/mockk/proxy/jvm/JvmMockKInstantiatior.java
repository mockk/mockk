package io.mockk.proxy.jvm;

import io.mockk.agent.MockKInstantiatior;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import static io.mockk.proxy.jvm.JvmMockKProxyMaker.log;
import static net.bytebuddy.implementation.MethodDelegation.to;

public class JvmMockKInstantiatior implements MockKInstantiatior {
    private final TypeCache<CacheKey> instanceProxyClassCache;

    private static final Object BOOTSTRAP_MONITOR = new Object();

    private final ObjenesisStd objenesis;

    private final Map<Class<?>, ObjectInstantiator<?>> instantiators = Collections.synchronizedMap(new WeakHashMap<Class<?>, ObjectInstantiator<?>>());

    private final ByteBuddy byteBuddy;

    public  JvmMockKInstantiatior() {
        objenesis = new ObjenesisStd(true);
        instanceProxyClassCache = new TypeCache<CacheKey>(TypeCache.Sort.SOFT);
        byteBuddy = new ByteBuddy()
                .with(TypeValidation.DISABLED);
    }


    @Override
    public <T> T instance(Class<T> cls) {
        if (!Modifier.isFinal(cls.getModifiers())) {
            try {
                T instance = instantiateViaProxy(cls);
                if (instance != null) {
                    return instance;
                }
            } catch (Exception ex) {
                log.trace(ex, "Failed to instantiate via proxy " + cls + ". " +
                        "Doing objenesis instantiation");
            }
        }


        return instanceViaObjenesis(cls);
    }

    private <T> T instantiateViaProxy(final Class<T> cls) {
        log.trace("Instantiating " + cls + " via subclass proxy");

        final ClassLoader classLoader = cls.getClassLoader();
        Object monitor = classLoader == null ? BOOTSTRAP_MONITOR : classLoader;
        Class<?> proxyCls =
                instanceProxyClassCache.findOrInsert(classLoader,
                        new CacheKey(cls, new Class[0]),
                        new Callable<Class<?>>() {
                            @Override
                            public Class<?> call() {
                                return byteBuddy.subclass(cls)
                                        .make()
                                        .load(classLoader)
                                        .getLoaded();
                            }
                        }, monitor);

        return cls.cast(instanceViaObjenesis(proxyCls));
    }


    @SuppressWarnings("unchecked")
    private <T> T instanceViaObjenesis(Class<T> clazz) {
        log.trace("Creating new empty instance of " + clazz);
        ObjectInstantiator<?> inst = instantiators.get(clazz);
        if (inst == null) {
            inst = objenesis.getInstantiatorOf(clazz);
            instantiators.put(clazz, inst);
        }
        return clazz.cast(inst.newInstance());
    }
}
