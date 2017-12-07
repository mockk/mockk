package io.mockk.common

import io.mockk.MockKGateway.Clearer

class CommonClearer(val stubRepository: StubRepository) : Clearer {
    override fun clear(mocks: Array<out Any>, answers: Boolean, recordedCalls: Boolean, childMocks: Boolean) {
        for (mock in mocks) {
            stubRepository.stubFor(mock).clear(answers, recordedCalls, childMocks)
        }
    }
}
