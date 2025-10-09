package io.mockk.impl.instantiation

import java.util.*

class RefCounterMap<T> {
    val counter = WeakHashMap<T, Int>()

    fun incrementRefCnt(cls: T) =
        synchronized(counter) {
            val cnt = counter[cls] ?: 0
            counter[cls] = cnt + 1
            cnt == 0
        }

    fun decrementRefCnt(cls: T) =
        synchronized(counter) {
            val cnt = counter[cls] ?: return true
            counter[cls] = cnt - 1
            cnt == 1
        }

}