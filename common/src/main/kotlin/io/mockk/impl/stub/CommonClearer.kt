package io.mockk.impl.stub

import io.mockk.MockKGateway.Clearer
import io.mockk.impl.log.Logger
import io.mockk.impl.log.SafeLog

class CommonClearer(
    val stubRepository: StubRepository,
    val safeLog: SafeLog
) : Clearer {
    val log = safeLog(Logger<CommonClearer>())

    override fun clear(mocks: Array<out Any>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        log.debug { "Clearing ${mocks.contentToString()} mocks" }
        for (mock in mocks) {
            stubRepository.stubFor(mock).clear(answers, recordedCalls, childMocks)
        }
    }
}
