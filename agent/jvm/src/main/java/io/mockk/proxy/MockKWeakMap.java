package io.mockk.proxy;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MockKWeakMap<K, V> implements Map<K, V> {
    private final Map<Object, V> map = new ConcurrentHashMap<Object, V>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return map.get(new StrongKey<K>((K) key));
    }

    public V put(K key, V value) {
        expunge();
        return map.put(new WeakKey<K>(key, queue), value);
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        expunge();
        return map.remove(new StrongKey<K>((K) key));
    }


    private void expunge() {
        Reference<?> ref;
        while ((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }

    private static class WeakKey<K> extends WeakReference<K> {
        private final int hashCode;

        public WeakKey(K key, ReferenceQueue<K> queue) {
            super(key, queue);
            hashCode = System.identityHashCode(key);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else {
                K key = get();
                if (key != null) {
                    if (o instanceof WeakKey<?>) {
                        return key == ((WeakKey<?>) o).get();
                    } else if (o instanceof StrongKey<?>) {
                        return key == ((StrongKey<?>) o).get();
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static class StrongKey<K> {
        private final int hashCode;
        private final K key;

        public StrongKey(K key) {
            this.key = key;
            hashCode = System.identityHashCode(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else {
                K key = get();
                if (key != null) {
                    if (o instanceof WeakKey<?>) {
                        return key == ((WeakKey<?>) o).get();
                    } else if (o instanceof StrongKey<?>) {
                        return key == ((StrongKey<?>) o).get();
                    }
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public K get() {
            return key;
        }
    }


    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("putAll");
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("keySet");
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("entrySet");
    }

}
