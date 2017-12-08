package io.mockk.impl.stub

import io.mockk.impl.InternalPlatform
import io.mockk.MockKException

class StubRepository {
    val stubs = InternalPlatform.weakMap<Any, Stub>()

    fun stubFor(mock: Any): Stub = stubs[mock]
            ?: throw MockKException("can't find stub for $mock")

    fun add(mock: Any, stub: Stub) {
        stubs.put(mock, stub)
    }

    operator fun get(mock: Any): Stub? = stubs.get(mock)

}

