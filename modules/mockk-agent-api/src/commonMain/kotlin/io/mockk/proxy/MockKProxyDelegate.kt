package io.mockk.proxy

/**
 * Implemented by proxies MockK builds in place of an instance of a hidden class, such as a
 * Kotlin 2 lambda compiled with `invokedynamic`
 * ([#1313](https://github.com/mockk/mockk/issues/1313)).
 *
 * Hidden classes can be neither subclassed nor instrumented, so the proxy implements the
 * hidden class's interfaces instead and answers un-stubbed "call original" requests by
 * forwarding them to the original instance stored in the proxy.
 */
interface MockKProxyDelegate {
    fun mockkGetOriginal(): Any?

    fun mockkSetOriginal(original: Any?)
}
