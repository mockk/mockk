package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.impl.InternalPlatform
import io.mockk.impl.MultiNotifier.Session
import io.mockk.impl.WeakRef
import io.mockk.impl.log.SafeToString

class StubRepository(
    val safeToString: SafeToString,
) {
    private val stubs = InternalPlatform.weakMap<Any, WeakRef>()
    private val recordCallMultiNotifier = InternalPlatform.multiNotifier()

    fun stubFor(mock: Any): Stub =
        get(mock)
            ?: throw MockKException(safeToString.exec { "can't find stub $mock" })

    fun add(
        mock: Any,
        stub: Stub,
    ) {
        stubs[mock] = InternalPlatform.weakRef(stub)
    }

    fun remove(mock: Any) = stubs.remove(mock)?.value as? Stub

    operator fun get(mock: Any): Stub? = stubs[mock]?.value as? Stub

    val allStubs: List<Stub>
        get() = stubs.values.mapNotNull { it.value as? Stub }

    internal fun clear(
        currentThreadOnly: Boolean,
        excludeMocks: List<Any>,
    ) {
        val excludePlatformMockKeys = InternalPlatform.getMockKeys(excludeMocks)
        val currentThreadId = Thread.currentThread().id

        // Recursively collect all children keys
        val excludeMockKeys = mutableSetOf<Any>()
        excludeMockKeys.addAll(excludePlatformMockKeys)
        val queue = ArrayDeque(excludePlatformMockKeys)
        while (queue.isNotEmpty()) {
            val parentKey = queue.removeFirst()
            val childrenKeys =
                (stubs[parentKey]?.value as? MockKStub)?.getChildrenKeys() ?: emptyList()
            for (childKey in childrenKeys) {
                if (excludeMockKeys.add(childKey)) {
                    queue.add(childKey)
                }
            }
        }

        stubs.removeIf { key, stub ->
            key !in excludeMockKeys && (!currentThreadOnly || (stub.value as? Stub)?.threadId == currentThreadId)
        }
    }

    fun notifyCallRecorded(stub: MockKStub) {
        recordCallMultiNotifier.notify(stub)
    }

    fun openRecordCallAwaitSession(
        stubs: List<Stub>,
        timeout: Long,
    ): Session = recordCallMultiNotifier.openSession(stubs, timeout)
}
