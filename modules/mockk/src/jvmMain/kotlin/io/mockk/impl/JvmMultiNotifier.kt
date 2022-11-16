package io.mockk.impl

import io.mockk.impl.MultiNotifier.Session
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class JvmMultiNotifier : MultiNotifier {
    val lock = ReentrantLock()
    val condition: Condition = lock.newCondition()

    val conditionMet = mutableSetOf<Ref>()
    val counters = mutableMapOf<Ref, Int>()

    override fun openSession(keys: List<Any>, timeout: Long): Session {
        val start = time()
        lock.withLock {
            changeCounters(keys, 1)
        }

        return SessionImpl(start, timeout, keys)
    }

    inner class SessionImpl(
        private val start: Long,
        private val timeout: Long,
        private val keys: List<Any>
    ) : Session {
        override fun wait(): Boolean {
            var ret = false
            lock.withLock {
                while (true) {
                    val passed = time() - start
                    if (passed >= timeout) {
                        break
                    }
                    if (checkAnyConditionsMet(keys)) {
                        ret = true
                        break
                    }
                    if (!condition.await(timeout - passed, TimeUnit.MILLISECONDS)) {
                        break
                    }
                }
            }
            return ret
        }

        override fun close() {
            lock.withLock {
                changeCounters(keys, -1)
            }
        }
    }

    private fun checkAnyConditionsMet(keys: List<Any>) = keys.any { InternalPlatform.ref(it) in conditionMet }

    private fun time() = System.currentTimeMillis()

    private fun changeCounters(keys: List<Any>, delta: Int) {
        keys.forEach {
            val ref = InternalPlatform.ref(it)
            val value = counters.getOrElse(ref) { 0 } + delta
            if (value == 0) {
                conditionMet.remove(ref)
                counters.remove(ref)
            } else {
                counters[ref] = value
            }
        }
    }

    override fun notify(key: Any) {
        lock.withLock {
            val ref = InternalPlatform.ref(key)
            if (ref in counters) {
                conditionMet.add(ref)
            }
            condition.signalAll()
        }
    }
}
