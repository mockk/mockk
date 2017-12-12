package io.mockk.impl.recording

import io.mockk.impl.InternalPlatform
import io.mockk.MockKException
import io.mockk.Ref
import io.mockk.impl.log.Logger
import io.mockk.impl.recording.states.RecordingCallRecorderState
import kotlin.reflect.KClass

class TemporaryMocks {
    val mocks = mutableListOf<Ref>()
    val temporaryMocks = mutableMapOf<KClass<*>, Any>()

    fun childMock(retType: KClass<*>, temporaryMockFactory: () -> Any): Any? =
            try {
                val mock = temporaryMocks[retType]

                if (mock != null) {
                    mock
                } else {
                    val child = temporaryMockFactory()

                    mocks.add(InternalPlatform.ref(child))
                    temporaryMocks[retType] = child

                    child
                }
            } catch (ex: MockKException) {
                log.trace(ex) { "Returning 'null' for a final class assuming it is last in a call chain" }
                null
            }

    fun requireNoArgIsChildMock(args: List<Any?>) {
        if (mocks.any { mock -> args.any { it === mock } }) {
            throw MockKException("Passing child mocks to arguments is prohibited")
        }
    }

    companion object {
        val log = Logger<TemporaryMocks>()
    }
}