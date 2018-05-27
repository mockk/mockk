package io.mockk.proxy.android;

import android.os.AsyncTask;
import android.util.ArraySet;
import io.mockk.proxy.MockKInvocationHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * A map mock -> adapter that holds weak references to the mocks and cleans them up when a
 * stale reference is found.
 */
public class AndroidMockKMap extends ReferenceQueue<Object>
        implements Map<Object, MockKInvocationHandler> {
    private static final int MIN_CLEAN_INTERVAL_MILLIS = 16000;
    private static final int MAX_GET_WITHOUT_CLEAN = 16384;

    private final Object lock = new Object();
    private StrongKey cachedKey;

    private HashMap<WeakKey, MockKInvocationHandler> adapters = new HashMap<>();

    /**
     * The time we issues the last cleanup
     */
    private long mLastCleanup = 0;

    /**
     * If {@link #cleanStaleReferences} is currently cleaning stale references out of
     * {@link #adapters}
     */
    private boolean isCleaning = false;

    /**
     * The number of time {@link #get} was called without cleaning up stale references.
     * {@link #get} is a method that is called often.
     * <p>
     * We need to do periodic cleanups as we might never look at mocks at higher indexes and
     * hence never realize that their references are stale.
     */
    private int getCount = 0;

    /**
     * Try to get a recycled cached key.
     *
     * @param obj the reference the key wraps
     * @return The recycled cached key or a new one
     */
    private StrongKey createStrongKey(Object obj) {
        synchronized (lock) {
            if (cachedKey == null) {
                cachedKey = new StrongKey();
            }

            cachedKey.obj = obj;
            StrongKey newKey = cachedKey;
            cachedKey = null;

            return newKey;
        }
    }

    /**
     * Recycle a key. The key should not be used afterwards
     *
     * @param key The key to recycle
     */
    private void recycleStrongKey(StrongKey key) {
        synchronized (lock) {
            cachedKey = key;
        }
    }

    @Override
    public int size() {
        return adapters.size();
    }

    @Override
    public boolean isEmpty() {
        return adapters.isEmpty();
    }

    @SuppressWarnings("CollectionIncompatibleType")
    @Override
    public boolean containsKey(Object mock) {
        synchronized (lock) {
            StrongKey key = createStrongKey(mock);
            boolean containsKey = adapters.containsKey(key);
            recycleStrongKey(key);

            return containsKey;
        }
    }

    @Override
    public boolean containsValue(Object adapter) {
        synchronized (lock) {
            return adapters.containsValue(adapter);
        }
    }

    @SuppressWarnings("CollectionIncompatibleType")
    @Override
    public MockKInvocationHandler get(Object mock) {
        synchronized (lock) {
            if (getCount > MAX_GET_WITHOUT_CLEAN) {
                cleanStaleReferences();
                getCount = 0;
            } else {
                getCount++;
            }

            StrongKey key = createStrongKey(mock);
            MockKInvocationHandler adapter = adapters.get(key);
            recycleStrongKey(key);

            return adapter;
        }
    }

    /**
     * Remove entries that reference a stale mock from {@link #adapters}.
     */
    private void cleanStaleReferences() {
        synchronized (lock) {
            if (isCleaning) {
                return;
            }

            if (System.currentTimeMillis() - MIN_CLEAN_INTERVAL_MILLIS < mLastCleanup) {
                return;
            }

            isCleaning = true;

            AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        while (true) {
                            Reference<?> ref = AndroidMockKMap.this.poll();
                            if (ref == null) {
                                break;
                            }

                            adapters.remove(ref);
                        }

                        mLastCleanup = System.currentTimeMillis();
                        isCleaning = false;
                    }
                }
            });
        }
    }

    @Override
    public MockKInvocationHandler put(Object mock, MockKInvocationHandler adapter) {
        synchronized (lock) {
            MockKInvocationHandler oldValue = remove(mock);
            adapters.put(new WeakKey(mock), adapter);

            return oldValue;
        }
    }

    @SuppressWarnings("CollectionIncompatibleType")
    @Override
    public MockKInvocationHandler remove(Object mock) {
        synchronized (lock) {
            StrongKey key = createStrongKey(mock);
            MockKInvocationHandler adapter = adapters.remove(key);
            recycleStrongKey(key);

            return adapter;
        }
    }

    @Override
    public void putAll(Map<?, ? extends MockKInvocationHandler> map) {
        synchronized (lock) {
            for (Entry<?, ? extends MockKInvocationHandler> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void clear() {
        synchronized (lock) {
            adapters.clear();
        }
    }

    @Override
    public Set<Object> keySet() {
        synchronized (lock) {
            Set<Object> mocks = new ArraySet<>(adapters.size());

            boolean hasStaleReferences = false;
            for (WeakKey key : adapters.keySet()) {
                Object mock = key.get();

                if (mock == null) {
                    hasStaleReferences = true;
                } else {
                    mocks.add(mock);
                }
            }

            if (hasStaleReferences) {
                cleanStaleReferences();
            }

            return mocks;
        }
    }

    @Override
    public Collection<MockKInvocationHandler> values() {
        synchronized (lock) {
            return adapters.values();
        }
    }

    @Override
    public Set<Entry<Object, MockKInvocationHandler>> entrySet() {
        synchronized (lock) {
            Set<Entry<Object, MockKInvocationHandler>> entries = new ArraySet<>(
                    adapters.size());

            boolean hasStaleReferences = false;
            for (Entry<WeakKey, MockKInvocationHandler> entry : adapters.entrySet()) {
                Object mock = entry.getKey().get();

                if (mock == null) {
                    hasStaleReferences = true;
                } else {
                    entries.add(new AbstractMap.SimpleEntry<>(mock, entry.getValue()));
                }
            }

            if (hasStaleReferences) {
                cleanStaleReferences();
            }

            return entries;
        }
    }

    public boolean isInternalHashMap(@NotNull Object instance) {
        return adapters == instance || this == instance;
    }

    /**
     * A weakly referencing wrapper to a mock.
     * <p>
     * Only equals other weak or strong keys where the mock is the same.
     */
    private class WeakKey extends WeakReference<Object> {
        private final int hashCode;

        private WeakKey(/*@NonNull*/ Object obj) {
            super(obj, AndroidMockKMap.this);

            // Cache the hashcode as the referenced object might disappear
            hashCode = System.identityHashCode(obj);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (other == null) {
                return false;
            }

            // Checking hashcode is cheap
            if (other.hashCode() != hashCode) {
                return false;
            }

            Object obj = get();

            if (obj == null) {
                cleanStaleReferences();
                return false;
            }

            if (other instanceof WeakKey) {
                Object otherObj = ((WeakKey) other).get();

                if (otherObj == null) {
                    cleanStaleReferences();
                    return false;
                }

                return obj == otherObj;
            } else if (other instanceof StrongKey) {
                Object otherObj = ((StrongKey) other).obj;
                return obj == otherObj;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    /**
     * A strongly referencing wrapper to a mock.
     * <p>
     * Only equals other weak or strong keys where the mock is the same.
     */
    private class StrongKey {
        /*@NonNull*/ private Object obj;

        @Override
        public boolean equals(Object other) {
            if (other instanceof WeakKey) {
                Object otherObj = ((WeakKey) other).get();

                if (otherObj == null) {
                    cleanStaleReferences();
                    return false;
                }

                return obj == otherObj;
            } else if (other instanceof StrongKey) {
                return this.obj == ((StrongKey) other).obj;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(obj);
        }
    }
}
