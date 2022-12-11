package io.mockk.proxy.jvm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

public class ClassLoadingStrategyChooser {
    private static final Method PRIVATE_LOOKUP_IN;
    private static final Object LOOKUP;

    static {
        Method privateLookupIn;
        Object lookup;
        try {
            Class<?> methodHandles = Class.forName("java.lang.invoke.MethodHandles");
            lookup = methodHandles.getMethod("lookup").invoke(null);
            privateLookupIn = methodHandles.getMethod(
                "privateLookupIn",
                Class.class,
                Class.forName("java.lang.invoke.MethodHandles$Lookup")
            );
        } catch (Exception e) {
            privateLookupIn = null;
            lookup = null;
        }
        PRIVATE_LOOKUP_IN = privateLookupIn;
        LOOKUP = lookup;
    }

    public static <T> ClassLoadingStrategy<ClassLoader> chooseClassLoadingStrategy(Class<T> type) {
        try {
            final ClassLoadingStrategy<ClassLoader> strategy;
            if (!type.getName().startsWith("java.") &&
                !type.getName().startsWith("javax.") &&
                    ClassInjector.UsingLookup.isAvailable() &&
                PRIVATE_LOOKUP_IN != null && LOOKUP != null) {
                Object privateLookup = PRIVATE_LOOKUP_IN.invoke(null, type, LOOKUP);
                strategy = ClassLoadingStrategy.UsingLookup.of(privateLookup);
            } else if (ClassInjector.UsingReflection.isAvailable()) {
                strategy = ClassLoadingStrategy.Default.INJECTION.with(type.getProtectionDomain());
            } else {
                strategy = ClassLoadingStrategy.Default.WRAPPER.with(type.getProtectionDomain());
            }
            return strategy;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new IllegalStateException(
                "Failed to invoke 'privateLookupIn' method from java.lang.invoke.MethodHandles$Lookup.",
                e
            );
        }
    }
}
