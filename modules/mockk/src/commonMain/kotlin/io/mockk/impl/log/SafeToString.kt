package io.mockk.impl.log

import io.mockk.impl.recording.CommonCallRecorder

class SafeToString(val callrecorderGetter: () -> CommonCallRecorder) {
    operator fun invoke(logger: Logger): Logger = SafeLogger(logger, callrecorderGetter)

    fun <T> exec(block: () -> T): T = callrecorderGetter().safeExec(block)
}
