package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.impl.InternalPlatform
import io.mockk.impl.WeakRef
import io.mockk.impl.log.SafeLog

class StubRepository(val safeLog: SafeLog) {
    val stubs = InternalPlatform.weakMap<Any, WeakRef>()

    fun stubFor(mock: Any): Stub = get(mock)
            ?: throw MockKException(safeLog.exec { "can't find stub $mock" })

    fun add(mock: Any, stub: Stub) {
        stubs[mock] = InternalPlatform.weakRef(stub)
    }

    fun remove(mock: Any) = stubs.remove(mock)?.value as? Stub

    operator fun get(mock: Any): Stub? = stubs[mock]?.value as? Stub

}

