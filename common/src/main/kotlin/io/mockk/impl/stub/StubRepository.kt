package io.mockk.impl.stub

import io.mockk.MockKException
import io.mockk.impl.InternalPlatform
import io.mockk.impl.log.SafeLog

class StubRepository(val safeLog: SafeLog) {
    val stubs = InternalPlatform.weakMap<Any, Stub>()

    fun stubFor(mock: Any): Stub = stubs[mock]
            ?: throw MockKException(safeLog.exec { "can't find stub $mock" })

    fun add(mock: Any, stub: Stub) {
        stubs.put(mock, stub)
    }

    fun remove(mock: Any) {
        stubs.remove(mock)
    }

    operator fun get(mock: Any): Stub? = stubs.get(mock)

}

