package io.mockk.proxy.jvm;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

final class CacheKey {
    private final Class<?> clazz;
    private final Set<Class<?>> interfaces;

    CacheKey(Class<?> clazz, Class<?>[] interfaces) {
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
