package io.mockk.impl.log

import io.mockk.impl.recording.CommonCallRecorder

class SafeLog(val callrecorderGetter: () -> CommonCallRecorder) {
    operator fun invoke(logger: Logger): Logger {
        return SafeLogger(logger, callrecorderGetter)
    }

    fun <T> exec(block: () -> T): T = callrecorderGetter().safeExec(block)
}