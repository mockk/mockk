package io.mockk.agent.inline;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WeakConcurrentHashMap<K, V> extends ReferenceQueue<K> {
    private final Map<Object, V> map = new ConcurrentHashMap<Object, V>();

    public V get(K key) {
        return map.get(new StrongKey<K>(key));
    }

    public void put(K key, V value) {
        expunge();
        map.put(new WeakKey<K>(key, this), value);
    }

    private void expunge() {
        Reference<?> ref;
        while ((ref = poll()) != null) {
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
            if (o instanceof WeakKey<?>) {
                return get() == ((WeakKey<?>) o).get();
            } else if (o instanceof StrongKey<?>){
                return get() == ((StrongKey<?>)o).get();
            } else {
                return false;
            }
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
            if (o instanceof WeakKey<?>) {
                return get() == ((WeakKey<?>) o).get();
            } else if (o instanceof StrongKey<?>){
                return get() == ((StrongKey<?>)o).get();
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        public K get() {
            return key;
        }
    }

}
