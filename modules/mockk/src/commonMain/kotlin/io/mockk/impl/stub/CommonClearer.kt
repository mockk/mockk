package io.mockk.impl.stub

import io.mockk.MockKGateway
import io.mockk.MockKGateway.Clearer
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeToString

class CommonClearer(
    val stubRepository: StubRepository,
    val safeToString: SafeToString
) : Clearer {
    val log = safeToString(Logger<CommonClearer>())

    override fun clear(mocks: Array<out Any>, options: MockKGateway.ClearOptions) {
        log.debug { "Clearing ${mocks.contentToString()} mocks $options" }
        for (mock in mocks) {
            stubRepository.stubFor(mock).clear(options)
        }
    }

    override fun clearAll(options: MockKGateway.ClearOptions, currentThreadOnly: Boolean) {
        val currentThreadId = Thread.currentThread().id
        stubRepository.allStubs.forEach {
            if (currentThreadOnly && currentThreadId != it.threadId) {
                return@forEach
            }
            it.clear(options)
        }
    }
}
