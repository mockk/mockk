package io.mockk.util

import java.lang.ref.WeakReference
import kotlin.test.assertEquals

private const val GC_ATTEMPTS = 50
private const val GC_PAUSE_MILLIS = 20L

fun assertGarbageCollected(
    refs: List<WeakReference<*>>,
    message: String,
) {
    repeat(GC_ATTEMPTS) {
        if (refs.all { it.get() == null }) return
        System.gc()
        Thread.sleep(GC_PAUSE_MILLIS)
    }
    assertEquals(0, refs.count { it.get() != null }, message)
}
