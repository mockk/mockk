package buildsrc.convention

import java.time.Duration

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
}
