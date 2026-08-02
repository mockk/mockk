package io.mockk.impl.stub

import io.mockk.MockKGateway
import io.mockk.MockKGateway.Clearer
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString

class CommonClearer(
    val stubRepository: StubRepository,
    val safeToString: SafeToString,
) : Clearer {
    val log = safeToString(Logger<CommonClearer>())

    override fun clear(
        mocks: Array<out Any>,
        options: MockKGateway.ClearOptions,
    ) {
        log.debug { "Clearing ${mocks.contentToString()} mocks $options" }
        for (mock in mocks) {
            stubRepository.stubFor(mock).clear(options)
        }
    }

    override fun clearAll(
        options: MockKGateway.ClearOptions,
        currentThreadOnly: Boolean,
    ) = clearAll(
        options = options,
        currentThreadOnly = currentThreadOnly,
        regularMocks = true,
        objectMocks = false,
        staticMocks = false,
        constructorMocks = false,
    )

    override fun clearAll(
        options: MockKGateway.ClearOptions,
        currentThreadOnly: Boolean,
        regularMocks: Boolean,
        objectMocks: Boolean,
        staticMocks: Boolean,
        constructorMocks: Boolean,
    ) {
        if (!regularMocks && !objectMocks && !staticMocks && !constructorMocks) {
            return
        }
        val currentThreadId = Thread.currentThread().id
        stubRepository.allStubs.forEach { stub ->
            if (currentThreadOnly && currentThreadId != stub.threadId) {
                return@forEach
            }
            val isRequested =
                when (stub.clearKind) {
                    ClearKind.REGULAR -> regularMocks
                    ClearKind.OBJECT -> objectMocks
                    ClearKind.STATIC -> staticMocks
                    ClearKind.CONSTRUCTOR -> constructorMocks
                    null -> false
                }
            if (isRequested) {
                stub.clear(options)
            }
        }
    }

    override fun clearAllStubsFromMemory(
        currentThreadOnly: Boolean,
        excludeMocks: List<Any>,
    ) = stubRepository.clear(currentThreadOnly, excludeMocks)
}
