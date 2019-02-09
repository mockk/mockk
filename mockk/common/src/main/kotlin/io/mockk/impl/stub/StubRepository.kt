package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.impl.InternalPlatform
import io.mockk.impl.MultiNotifier.Session
import io.mockk.impl.WeakRef
import io.mockk.impl.log.SafeToString

class StubRepository(
    val safeToString: SafeToString
) {
    private val stubs = InternalPlatform.weakMap<Any, WeakRef>()
    private val recordCallMultiNotifier = InternalPlatform.multiNotifier()

    fun stubFor(mock: Any): Stub = get(mock)
        ?: throw MockKException(safeToString.exec { "can't find stub $mock" })

    fun add(mock: Any, stub: Stub) {
        stubs[mock] = InternalPlatform.weakRef(stub)
    }

    fun remove(mock: Any) = stubs.remove(mock)?.value as? Stub

    operator fun get(mock: Any): Stub? = stubs[mock]?.value as? Stub

    val allStubs: List<Stub>
        get() = stubs.values.mapNotNull { it.value as? Stub }

    fun notifyCallRecorded(stub: MockKStub) {
        recordCallMultiNotifier.notify(stub)
    }

    fun openRecordCallAwaitSession(
        stubs: List<Stub>,
        timeout: Long
    ): Session {
        return recordCallMultiNotifier.openSession(stubs, timeout)
    }
}

