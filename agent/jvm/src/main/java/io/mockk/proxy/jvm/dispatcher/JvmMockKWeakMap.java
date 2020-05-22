package io.mockk.proxy.jvm.dispatcher;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JvmMockKWeakMap<K, V> implements Map<K, V> {
    private final Map<Object, V> target = new ConcurrentHashMap<Object, V>();
    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return target.get(new StrongKey<K>((K) key));
    }

    public V put(K key, V value) {
        expunge();
        return target.put(new WeakKey<K>(key, queue), value);
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        expunge();
        return target.remove(new StrongKey<K>((K) key));
    }


    private void expunge() {
        Reference<?> ref;
        while ((ref = queue.poll()) != null) {
            target.remove(ref);
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
        return target.size();
    }

    @Override
    public boolean isEmpty() {
        return target.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return target.containsValue(value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException("putAll");
    }

    @Override
    public void clear() {
        target.clear();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("keySet");
    }

    @Override
    public Collection<V> values() {
        return target.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("entrySet");
    }

    public Map<Object, V> getTarget() {
        return target;
    }
}
