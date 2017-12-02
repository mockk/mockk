package io.mockk

actual object InternalPlatform {
    actual fun identityHashCode(obj: Any): Int = System.identityHashCode(obj)
}