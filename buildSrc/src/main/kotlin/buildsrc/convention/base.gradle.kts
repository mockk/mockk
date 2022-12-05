package buildsrc.convention

import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    base
}

description =
    "Common build config that can be applied to any project. This should typically be language-independent."

if (project != rootProject) {
    group = rootProject.group
    version = rootProject.version
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(10))

    testLogging {
        // showCauses = true
        // showExceptions = true
        // showStackTraces = true
        // showStandardStreams = true
        events(
            TestLogEvent.STARTED,
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            // TestLogEvent.STANDARD_ERROR,
            // TestLogEvent.STANDARD_OUT,
        )
    }
}
