package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.time.Duration

plugins {
    base
}

// Common build config that can be applied to any project. This should typically be language-independent.

if (project != rootProject) {
    group = rootProject.group
    version = rootProject.version
}

tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(10))

    testLogging {
        // don't log console output - it's too noisy
        showCauses = false
        showExceptions = false
        showStackTraces = false
        showStandardStreams = false
        events(
            // only log test outcomes
            TestLogEvent.PASSED,
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            // TestLogEvent.STARTED,
            // TestLogEvent.STANDARD_ERROR,
            // TestLogEvent.STANDARD_OUT,
        )
    }
}

// Enforce reproducible builds: https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
